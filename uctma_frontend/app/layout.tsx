import "./globals.css";

export const metadata = {
  title: "UCTMA",
  description: "Universal Converter to Markdown Agent",
};

export default function RootLayout({
  children,
}: Readonly<{ children: any }>) {
  return (
    <html lang="tr">
      <body>{children}</body>
    </html>
  );
}
