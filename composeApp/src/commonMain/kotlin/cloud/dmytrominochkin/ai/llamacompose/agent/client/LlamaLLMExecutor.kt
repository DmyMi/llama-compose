package cloud.dmytrominochkin.ai.llamacompose.agent.client

import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor

class LlamaLLMExecutor(llmClient: LLMClient) : SingleLLMPromptExecutor(llmClient)
