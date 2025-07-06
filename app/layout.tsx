import './globals.css'
import type { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'Enhanced Music Player',
  description: 'A modern music player with Internet Archive integration',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body className="bg-gray-900 text-white">
        {children}
      </body>
    </html>
  )
}