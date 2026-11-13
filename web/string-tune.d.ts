import "react";

declare module "react" {
  interface HTMLAttributes<T> extends AriaAttributes, DOMAttributes<T> {
    string?: string;
    "string-factor"?: string;
    "string-cursor-target"?: string;
    "string-cursor"?: string;
    "string-id"?: string;
    "string-copy-from"?: string;
  }
}
