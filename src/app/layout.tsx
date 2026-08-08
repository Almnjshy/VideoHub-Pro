import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { Toaster } from "@/components/ui/toaster";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "VideoHub Pro Enterprise — منصة إدارة الوسائط",
  description: "تطبيق احترافي لإدارة وتنزيل المحتوى الرقمي بنظام الوحدات المستقلة والمشاركة الذكية",
  keywords: ["VideoHub", "تنزيل", "يوتيوب", "تيك توك", "فيسبوك", "إكس", "إدارة وسائط"],
  authors: [{ name: "VideoHub Pro" }],
  icons: {
    icon: "https://z-cdn.chatglm.cn/z-ai/static/logo.svg",
  },
  openGraph: {
    title: "VideoHub Pro Enterprise",
    description: "منصة إدارة الوسائط الاحترافية",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ar" dir="rtl" suppressHydrationWarning>
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased bg-background text-foreground`}
      >
        {children}
        <Toaster />
      </body>
    </html>
  );
}
