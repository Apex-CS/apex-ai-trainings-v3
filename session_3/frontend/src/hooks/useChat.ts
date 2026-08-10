import { useCallback, useEffect, useRef, useState } from 'react'
import { ChatApiError, sendChatMessage } from '../api/chat'
import {
  DEMO_USER_STORAGE_KEY,
  findDemoUser,
  type DemoUserId,
  type DemoUserProfile,
} from '../data/demoUsers'
import type { ChatMessage, CodeAttachment } from '../types/chat'

const CONVERSATION_KEY = 'example-company-chat-conversation-id'

function loadStoredDemoUser(): DemoUserProfile | null {
  const stored = sessionStorage.getItem(DEMO_USER_STORAGE_KEY) as DemoUserId | null
  if (!stored) return null
  return findDemoUser(stored) ?? null
}

function createMessage(
  role: ChatMessage['role'],
  content: string,
  warnings?: string[],
  attachmentFilename?: string,
): ChatMessage {
  return {
    id: crypto.randomUUID(),
    role,
    content,
    timestamp: new Date(),
    warnings,
    attachmentFilename,
  }
}

export function useChat() {
  const [messages, setMessages] = useState<ChatMessage[]>([
    createMessage(
      'assistant',
      'Hello! I am the Example Company AI assistant. I can help with finance, IT, marketing, and sales questions using internal documents, web search, or the company database. Attach a .py, .html, or .zip file if you want a code review.',
    ),
  ])
  const [conversationId, setConversationId] = useState<string | null>(() =>
    sessionStorage.getItem(CONVERSATION_KEY),
  )
  const [demoUser, setDemoUser] = useState<DemoUserProfile | null>(() => loadStoredDemoUser())
  const [characterSelectOpen, setCharacterSelectOpen] = useState(() => loadStoredDemoUser() === null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [warnings, setWarnings] = useState<string[]>([])
  const messagesEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isLoading])

  const selectDemoUser = useCallback((user: DemoUserProfile) => {
    setDemoUser(user)
    sessionStorage.setItem(DEMO_USER_STORAGE_KEY, user.id)
    setCharacterSelectOpen(false)
  }, [])

  const sendMessage = useCallback(
    async (text: string, attachment?: CodeAttachment) => {
      const trimmed = text.trim()
      if ((!trimmed && !attachment) || isLoading || !demoUser) return

      const outgoingMessage = trimmed || 'Please review the attached code.'
      setError(null)
      setWarnings([])
      setMessages((prev) => [
        ...prev,
        createMessage('user', outgoingMessage, undefined, attachment?.filename),
      ])
      setIsLoading(true)

      try {
        const response = await sendChatMessage({
          message: outgoingMessage,
          conversationId: conversationId ?? undefined,
          codeToReview: attachment,
          demoUser: demoUser.id,
        })

        const responseWarnings = response.warnings ?? []
        setConversationId(response.conversationId)
        sessionStorage.setItem(CONVERSATION_KEY, response.conversationId)
        setWarnings(responseWarnings)
        setMessages((prev) => [
          ...prev,
          createMessage('assistant', response.answer, responseWarnings),
        ])
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Something went wrong'
        const isHardError = !(err instanceof ChatApiError) || err.type === 'hard'
        if (isHardError) {
          setError(message)
        } else {
          setWarnings([message])
        }
        setMessages((prev) => [
          ...prev,
          createMessage(
            'assistant',
            isHardError
              ? `Sorry, I could not process that request. ${message}`
              : `I ran into a partial issue, but here is what I could determine. ${message}`,
            isHardError ? undefined : [message],
          ),
        ])
      } finally {
        setIsLoading(false)
      }
    },
    [conversationId, demoUser, isLoading],
  )

  const startNewChat = useCallback(() => {
    sessionStorage.removeItem(CONVERSATION_KEY)
    sessionStorage.removeItem(DEMO_USER_STORAGE_KEY)
    setConversationId(null)
    setDemoUser(null)
    setCharacterSelectOpen(true)
    setError(null)
    setWarnings([])
    setMessages([
      createMessage(
        'assistant',
        'Started a new conversation. Choose your demo character, then ask me about finance, IT, marketing, sales, internal company documents, or attach code for review.',
      ),
    ])
  }, [])

  return {
    messages,
    isLoading,
    error,
    warnings,
    demoUser,
    characterSelectOpen,
    selectDemoUser,
    sendMessage,
    startNewChat,
    messagesEndRef,
  }
}
