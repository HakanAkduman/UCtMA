import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { Modal } from "./Modal";

type PreviewDialogProps = {
  fileName: string;
  content: string | null;
  loading: boolean;
  error: string | null;
  onClose: () => void;
};

export function PreviewDialog({
  fileName,
  content,
  loading,
  error,
  onClose,
}: PreviewDialogProps) {
  return (
    <Modal title={`Önizleme: ${fileName}`} onClose={onClose}>
      <div className="max-h-[70vh] overflow-auto rounded-lg border border-slate-200 p-5 text-sm dark:border-slate-700">
        {loading ? (
          <p className="text-slate-500">Önizleme yükleniyor...</p>
        ) : error ? (
          <p role="alert" className="text-red-700 dark:text-red-300">{error}</p>
        ) : content ? (
          <ReactMarkdown
            remarkPlugins={[remarkGfm]}
            components={{
              h1: ({ children }) => <h1 className="mb-4 text-2xl font-bold">{children}</h1>,
              h2: ({ children }) => <h2 className="mb-3 mt-6 text-xl font-semibold">{children}</h2>,
              h3: ({ children }) => <h3 className="mb-2 mt-5 text-lg font-semibold">{children}</h3>,
              p: ({ children }) => <p className="mb-4 whitespace-pre-wrap leading-7">{children}</p>,
              ul: ({ children }) => <ul className="mb-4 list-disc space-y-1 pl-6">{children}</ul>,
              ol: ({ children }) => <ol className="mb-4 list-decimal space-y-1 pl-6">{children}</ol>,
              li: ({ children }) => <li>{children}</li>,
              blockquote: ({ children }) => (
                <blockquote className="mb-4 border-l-4 border-slate-300 pl-4 text-slate-600 dark:border-slate-600 dark:text-slate-300">
                  {children}
                </blockquote>
              ),
              a: ({ href, children }) => (
                <a className="text-blue-700 underline dark:text-blue-300" href={href} target="_blank" rel="noreferrer">
                  {children}
                </a>
              ),
              pre: ({ children }) => (
                <pre className="mb-4 overflow-x-auto rounded bg-slate-100 p-3 dark:bg-slate-800">
                  {children}
                </pre>
              ),
              code: ({ children }) => (
                <code className="rounded bg-slate-100 px-1 py-0.5 dark:bg-slate-800">
                  {children}
                </code>
              ),
              table: ({ children }) => (
                <table className="mb-4 w-full border-collapse text-left">{children}</table>
              ),
              th: ({ children }) => <th className="border border-slate-300 p-2 dark:border-slate-600">{children}</th>,
              td: ({ children }) => <td className="border border-slate-300 p-2 dark:border-slate-600">{children}</td>,
              hr: () => <hr className="my-6 border-slate-200 dark:border-slate-700" />,
            }}
          >
            {content}
          </ReactMarkdown>
        ) : (
          <p className="text-slate-500">Önizlenecek içerik bulunamadı.</p>
        )}
      </div>
    </Modal>
  );
}
