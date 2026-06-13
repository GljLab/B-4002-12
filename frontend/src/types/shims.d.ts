declare module '@kangc/v-md-editor' {
  import type { DefineComponent, Plugin, App } from 'vue'

  export interface ToolbarConfigItem {
    name?: string
    title?: string
    icon?: string
    text?: string
    action?: () => void
  }

  export interface VMdEditorProps {
    modelValue?: string
    mode?: 'edit' | 'preview' | 'live'
    toolbar?: Array<string | ToolbarConfigItem>
    height?: string | number
    placeholder?: string
    disabled?: boolean
  }

  const VMdEditor: DefineComponent<VMdEditorProps> & {
    use: (theme: any, options?: any) => void
    install: (app: App, ...options: any[]) => void
  }

  const VMdPreview: DefineComponent

  export { VMdEditor, VMdPreview }

  const plugin: Plugin & {
    use: (theme: any, options?: any) => void
  }
  export default plugin
}

declare module '@kangc/v-md-editor/lib/theme/vuepress.js' {
  const theme: any
  export default theme
}

declare module '@kangc/v-md-editor/lib/theme/style/github.css' {}
declare module '@kangc/v-md-editor/lib/theme/style/vuepress.css' {}
declare module '@kangc/v-md-editor/lib/style/base-editor.css' {}

declare module 'markdown-it-emoji' {
  import type MarkdownIt from 'markdown-it'
  const emoji: (md: MarkdownIt) => void
  export default emoji
}

declare module 'markdown-it-task-lists' {
  import type MarkdownIt from 'markdown-it'
  interface TaskListsOptions {
    label?: boolean
    labelAfter?: boolean
  }
  const taskLists: (md: MarkdownIt, options?: TaskListsOptions) => void
  export default taskLists
}
