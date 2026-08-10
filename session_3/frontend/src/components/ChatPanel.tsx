import { ChatInput } from './ChatInput'
import { ChatMessage } from './ChatMessage'
import { useChat } from '../hooks/useChat'

export function ChatPanel() {
  const { messages, isLoading, error, warnings, sendMessage, startNewChat, messagesEndRef } = useChat()

  return (
    <div className="chat-layout">
      <header className="chat-header">
        <div>
          <p className="eyebrow">Example Company</p>
          <h1>Java-powered Agent for Vectorized Intelligence with Enterprise Response</h1>
          <p className="subtitle">
            Finance · IT · Marketing · Sales — RAG, web search, and SQL via LangGraph4j
          </p>
        </div>
        <button type="button" className="new-chat-btn" onClick={startNewChat} disabled={isLoading}>
          New chat
        </button>
      </header>

      <main className="chat-main">
        <div className="messages" role="log" aria-live="polite" aria-relevant="additions">
          {messages.map((message) => (
            <ChatMessage key={message.id} message={message} />
          ))}
          {isLoading && (
            <div className="typing-indicator" aria-label="Assistant is thinking">
              <span />
              <span />
              <span />
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>

        {warnings.length > 0 && (
          <div className="warning-banner" role="status">
            <p className="warning-banner-title">Some tools were unavailable</p>
            <ul>
              {warnings.map((warning) => (
                <li key={warning}>{warning}</li>
              ))}
            </ul>
          </div>
        )}

        {error && <p className="error-banner">{error}</p>}

        <ChatInput onSend={sendMessage} disabled={isLoading} />
      </main>
    </div>
  )
}
