import type { CodeAttachment } from '../types/chat'

export const MAX_ATTACHMENT_BYTES = 10 * 1024 * 1024

export const ACCEPTED_EXTENSIONS = [
  '.py', '.java', '.js', '.ts', '.tsx', '.jsx', '.html', '.htm', '.css', '.xml',
  '.yml', '.yaml', '.properties', '.md', '.sql', '.json', '.gradle', '.kt', '.go',
  '.rb', '.php', '.cs', '.vue', '.scss', '.less', '.sh', '.bat', '.c', '.cpp',
  '.h', '.hpp', '.env', '.rs', '.swift', '.toml', '.ini', '.gitignore', '.dockerignore',
  '.zip',
] as const

const ACCEPTED_FILENAMES = new Set([
  'dockerfile', 'makefile', 'readme', 'license', 'procfile',
])

export const ACCEPTED_FILE_INPUT = [
  ...ACCEPTED_EXTENSIONS,
  'application/zip',
].join(',')

export function isAcceptedAttachment(file: File): boolean {
  const lowerName = file.name.toLowerCase()
  const baseName = lowerName.includes('/')
    ? lowerName.slice(lowerName.lastIndexOf('/') + 1)
    : lowerName.includes('\\')
      ? lowerName.slice(lowerName.lastIndexOf('\\') + 1)
      : lowerName

  if (ACCEPTED_FILENAMES.has(baseName)) {
    return true
  }

  return ACCEPTED_EXTENSIONS.some((extension) => lowerName.endsWith(extension))
}

export function fileToAttachment(file: File): Promise<CodeAttachment> {
  if (!isAcceptedAttachment(file)) {
    return Promise.reject(
      new Error('This file type is not supported for code review.'),
    )
  }

  if (file.size > MAX_ATTACHMENT_BYTES) {
    return Promise.reject(new Error('Attachment must be 10 MB or smaller.'))
  }

  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const dataUrl = reader.result
      if (typeof dataUrl !== 'string') {
        reject(new Error('Failed to read attachment.'))
        return
      }

      const commaIndex = dataUrl.indexOf(',')
      if (commaIndex < 0) {
        reject(new Error('Failed to encode attachment.'))
        return
      }

      resolve({
        filename: file.name,
        contentType: file.type || 'application/octet-stream',
        encoding: 'base64',
        data: dataUrl.slice(commaIndex + 1),
      })
    }
    reader.onerror = () => reject(new Error('Failed to read attachment.'))
    reader.readAsDataURL(file)
  })
}
