import { useRef, useState, type FormEvent } from 'react'
import type { CodeAttachment } from '../types/chat'
import { ACCEPTED_FILE_INPUT, fileToAttachment } from '../utils/fileAttachment'

interface ChatInputProps {
  onSend: (message: string, attachment?: CodeAttachment) => void
  disabled?: boolean
  placeholder?: string
}

const SUGGESTIONS = [
  'What company data is available in the database?',
  'Review the attached code for OWASP Top 10 issues.',
  'Search the knowledge base for internal policies.',
]

export function ChatInput({ onSend, disabled, placeholder }: ChatInputProps) {
  const [input, setInput] = useState('')
  const [attachment, setAttachment] = useState<CodeAttachment | null>(null)
  const [attachmentError, setAttachmentError] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    if (!input.trim() && !attachment) return
    onSend(input, attachment ?? undefined)
    setInput('')
    setAttachment(null)
    setAttachmentError(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  const handleFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    setAttachmentError(null)
    try {
      const encoded = await fileToAttachment(file)
      setAttachment(encoded)
    } catch (error) {
      setAttachment(null)
      setAttachmentError(error instanceof Error ? error.message : 'Failed to attach file.')
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    }
  }

  const removeAttachment = () => {
    setAttachment(null)
    setAttachmentError(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  return (
    <div className="chat-input-area">
      <div className="suggestions">
        {SUGGESTIONS.map((suggestion) => (
          <button
            key={suggestion}
            type="button"
            className="suggestion-chip"
            disabled={disabled}
            onClick={() => onSend(suggestion)}
          >
            {suggestion}
          </button>
        ))}
      </div>

      {attachment && (
        <div className="attachment-preview" role="status">
          <span className="attachment-label">Attached for review:</span>
          <span className="attachment-name">{attachment.filename}</span>
          <button
            type="button"
            className="attachment-remove"
            onClick={removeAttachment}
            disabled={disabled}
            aria-label={`Remove attachment ${attachment.filename}`}
          >
            Remove
          </button>
        </div>
      )}

      {attachmentError && <p className="attachment-error">{attachmentError}</p>}

      <form className="chat-form" onSubmit={handleSubmit}>
        <input
          ref={fileInputRef}
          type="file"
          accept={ACCEPTED_FILE_INPUT}
          className="file-input"
          onChange={handleFileChange}
          disabled={disabled}
          aria-label="Attach code file"
        />
        <textarea
          value={input}
          onChange={(event) => setInput(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter' && !event.shiftKey) {
              event.preventDefault()
              handleSubmit(event)
            }
          }}
          placeholder={placeholder ?? 'Ask a question or attach code for review...'}
          rows={2}
          disabled={disabled}
          aria-label="Chat message"
        />
        <button type="submit" disabled={disabled || (!input.trim() && !attachment)}>
          {disabled ? 'Thinking...' : 'Send'}
        </button>
      </form>
    </div>
  )
}
