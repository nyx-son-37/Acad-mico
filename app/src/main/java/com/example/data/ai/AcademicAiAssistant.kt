package com.example.data.ai

import android.util.Log
import com.example.data.attachments.AttachmentStorageHelper
import com.example.domain.model.Assignment
import com.example.domain.model.AssignmentAttachment
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AiRequirementAnalysis(
    val requiresAttachment: Boolean,
    val requirementSummary: String,
    val requiredChecklist: List<String>,
    val rationale: String,
)

data class AiAttachmentEvaluation(
    val verdict: String,
    val meetsAttachmentRequirement: Boolean,
    val estimatedScorePercent: Int,
    val feedbackSummary: String,
    val strengths: List<String>,
    val gaps: List<String>,
    val nextSteps: List<String>,
)

class AcademicAiAssistant {

    private val requirementSchema = Schema.obj(
        properties = mapOf(
            "requiresAttachment" to Schema.boolean(
                description = "True if the assignment requires a physical/digital file or image attachment as evidence."
            ),
            "requirementSummary" to Schema.string(
                description = "Short, clear Portuguese specification of what attachment(s) the student must submit."
            ),
            "requiredChecklist" to Schema.array(
                items = Schema.string(),
                description = "Checklist of 2 to 5 concrete items the attachment must contain."
            ),
            "rationale" to Schema.string(
                description = "Brief Portuguese explanation of why this attachment is required for the assignment."
            ),
        )
    )

    private val evaluationSchema = Schema.obj(
        properties = mapOf(
            "verdict" to Schema.enumeration(
                values = listOf("approved", "needs_revision", "missing_requirements", "rejected"),
                description = "Overall AI verdict on the submitted attachment against the assignment requirements."
            ),
            "meetsAttachmentRequirement" to Schema.boolean(
                description = "Whether the attachment satisfies the required evidence/deliverable specification."
            ),
            "estimatedScorePercent" to Schema.integer(
                description = "Estimated score percentage from 0 to 100 based on quality and completeness."
            ),
            "feedbackSummary" to Schema.string(
                description = "Constructive, detailed academic evaluation summary in Portuguese."
            ),
            "strengths" to Schema.array(
                items = Schema.string(),
                description = "List of concrete strengths observed in the attachment in Portuguese."
            ),
            "gaps" to Schema.array(
                items = Schema.string(),
                description = "List of missing requirements, errors, or weak points in Portuguese."
            ),
            "nextSteps" to Schema.array(
                items = Schema.string(),
                description = "Actionable next steps for the student before submission in Portuguese."
            ),
        )
    )

    private val requirementModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = requirementSchema
            }
        )
    }

    private val evaluationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = evaluationSchema
            }
        )
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel(modelName = MODEL_NAME)
    }

    suspend fun analyzeAttachmentRequirements(
        institutionName: String,
        courseName: String,
        categoryName: String,
        title: String,
        description: String,
        maxPoints: Double
    ): AiRequirementAnalysis {
        val prompt = """
            Você é um assistente acadêmico exigente e estruturado.
            Analise os dados do trabalho abaixo e defina exatamente quais anexos o aluno DEVE enviar como evidência para que o trabalho seja aceito e avaliado.
            Responda em Português do Brasil.

            Instituição: $institutionName
            Disciplina: $courseName
            Categoria: $categoryName
            Título: $title
            Pontuação máxima: $maxPoints pontos
            Enunciado / Descrição: ${description.ifBlank { "Sem descrição detalhada fornecida." }}
        """.trimIndent()

        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                requirementModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("O modelo retornou uma resposta vazia.")
            val json = JSONObject(raw)
            AiRequirementAnalysis(
                requiresAttachment = json.optBoolean("requiresAttachment", true),
                requirementSummary = json.optString("requirementSummary", "Enviar documento ou imagem comprobatória."),
                requiredChecklist = json.optJSONArray("requiredChecklist")?.let { arr ->
                    (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
                } ?: emptyList(),
                rationale = json.optString("rationale", "")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Requirement analysis timed out", e)
            throw IllegalStateException("A análise de exigências da IA excedeu 60 segundos. Tente novamente.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Requirement analysis failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Falha ao consultar o assistente de IA.")
        }
    }

    suspend fun evaluateAttachment(
        institutionName: String,
        courseName: String,
        categoryName: String,
        assignment: Assignment,
        attachment: AssignmentAttachment
    ): AiAttachmentEvaluation {
        val prepared = AttachmentStorageHelper.prepareForAi(
            localFilePath = attachment.localFilePath,
            mimeType = attachment.mimeType,
            fileName = attachment.fileName,
            textNote = attachment.textNote
        )

        val promptHeader = """
            Você é um avaliador acadêmico rigoroso e construtivo.
            Sua tarefa é verificar se o anexo enviado atende às exigências do trabalho e avaliá-lo criticamente.
            Responda em Português do Brasil.

            Contexto do Trabalho:
            - Instituição: $institutionName
            - Disciplina: $courseName
            - Categoria: $categoryName
            - Título: ${assignment.title}
            - Pontuação máxima: ${assignment.maxPoints} pontos
            - Enunciado / Rubrica: ${assignment.description.ifBlank { "Não informado." }}
            - Exigência de Anexo: ${assignment.requiredAttachmentSpec.ifBlank { "Anexo pertinente ao trabalho." }}

            Metadados do Anexo Enviado:
            - Nome do arquivo: ${attachment.fileName}
            - Tipo MIME: ${attachment.mimeType}
            - Tamanho: ${attachment.sizeBytes} bytes
            ${if (prepared.extractedText.isNotBlank()) "\n${prepared.extractedText}" else ""}
            ${if (prepared.images.isNotEmpty()) "\nAs páginas/imagens do anexo seguem em anexo abaixo para inspeção visual." else ""}

            Avalie rigorosamente se o conteúdo realmente corresponde ao trabalho e atende à exigência.
        """.trimIndent()

        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                evaluationModel.generateContent(
                    content {
                        text(promptHeader)
                        prepared.images.forEach { bitmap ->
                            image(bitmap)
                        }
                    }
                )
            }
            val raw = response.text ?: throw IllegalStateException("O modelo retornou uma avaliação vazia.")
            val json = JSONObject(raw)
            AiAttachmentEvaluation(
                verdict = json.optString("verdict", "needs_revision"),
                meetsAttachmentRequirement = json.optBoolean("meetsAttachmentRequirement", false),
                estimatedScorePercent = json.optInt("estimatedScorePercent", 0).coerceIn(0, 100),
                feedbackSummary = json.optString("feedbackSummary", ""),
                strengths = json.optJSONArray("strengths")?.let { arr ->
                    (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
                } ?: emptyList(),
                gaps = json.optJSONArray("gaps")?.let { arr ->
                    (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
                } ?: emptyList(),
                nextSteps = json.optJSONArray("nextSteps")?.let { arr ->
                    (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
                } ?: emptyList(),
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Attachment evaluation timed out", e)
            throw IllegalStateException("A avaliação do anexo excedeu 60 segundos. Tente novamente.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Attachment evaluation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Falha na avaliação do anexo pela IA.")
        }
    }

    fun askAboutAssignmentStream(
        courseName: String,
        assignment: Assignment,
        question: String
    ): Flow<String> = flow {
        val prompt = """
            Você é o Assistente Acadêmico do aplicativo Acadêmico. Responda de forma objetiva, prática e em Português do Brasil.
            Disciplina: $courseName
            Trabalho: ${assignment.title} (vale ${assignment.maxPoints} pts)
            Enunciado: ${assignment.description}
            Exigência de anexo: ${assignment.requiredAttachmentSpec}
            Avaliação anterior da IA: ${assignment.aiSummary}

            Pergunta do estudante: $question
        """.trimIndent()

        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                chatModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("O assistente não retornou resposta.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Chat stream timed out", e)
            throw IllegalStateException("O assistente demorou mais de 60 segundos para responder.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Chat stream failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Erro ao conversar com o assistente.")
        }
    }

    private companion object {
        const val TAG = "AcademicAiAssistant"
        const val MODEL_NAME = "gemini-3.8-flash"
        const val AI_TIMEOUT_MS = 60_000L
    }
}
