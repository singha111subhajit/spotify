/** @type {import('next').NextConfig} */
const nextConfig = {
  experimental: {
    appDir: true,
  },
  images: {
    domains: ['archive.org', 'ia801502.us.archive.org', 'ia801401.us.archive.org', 'ia801604.us.archive.org'],
  },
}

module.exports = nextConfig