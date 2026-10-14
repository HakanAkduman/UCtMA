const backendUrl = process.env.BACKEND_URL || "http://backend-service:8080";

/** @type {import("next").NextConfig} */
const nextConfig = {
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${backendUrl}/uctma/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
