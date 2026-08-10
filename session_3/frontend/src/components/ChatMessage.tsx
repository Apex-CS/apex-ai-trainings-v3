import ReactMarkdown from 'react-markdown'
import type { ChatMessage as ChatMessageType } from '../types/chat'

interface ChatMessageProps {
  message: ChatMessageType
}

export function ChatMessage({ message }: ChatMessageProps) {
  const isUser = message.role === 'user'

  return (
    <article className={`message ${isUser ? 'message-user' : 'message-assistant'}`}>
      <div className="message-meta">
        <span className="message-role">{isUser ? 'You' : 'Assistant'}</span>
        <time dateTime={message.timestamp.toISOString()}>
          {message.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
        </time>
      </div>
      <div className="message-body">
        {message.attachmentFilename && (
          <p className="message-attachment">Attached: {message.attachmentFilename}</p>
        )}
        {isUser ? (
          <p>{message.content}</p>
        ) : (
          <ReactMarkdown>{message.content}</ReactMarkdown>
        )}
      </div>
    </article>
  )
}
