<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = withDefaults(
  defineProps<{
    modelValue: string
    placeholder?: string
    maxLength?: number
    loading?: boolean
    showCancel?: boolean
    replyToUsername?: string | null
  }>(),
  {
    placeholder: '写下你的想法...',
    maxLength: 5000,
    loading: false,
    showCancel: false,
    replyToUsername: null,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  submit: []
  cancel: []
}>()

const content = ref(props.modelValue)
const isFocused = ref(false)

watch(
  () => props.modelValue,
  (newVal) => {
    content.value = newVal
  },
)

const charCount = computed(() => content.value.length)
const isOverLimit = computed(() => charCount.value > props.maxLength)
const isNearLimit = computed(() => charCount.value > props.maxLength * 0.9)

function handleInput(event: Event) {
  const target = event.target as HTMLTextAreaElement
  content.value = target.value
  emit('update:modelValue', content.value)
}

function handleSubmit() {
  if (!content.value.trim()) {
    ElMessage.warning('内容不能为空')
    return
  }
  if (isOverLimit.value) {
    ElMessage.warning(`内容不能超过${props.maxLength}字`)
    return
  }
  emit('submit')
}

function handleCancel() {
  content.value = ''
  emit('update:modelValue', '')
  emit('cancel')
}

function handleFocus() {
  isFocused.value = true
}

function handleBlur() {
  isFocused.value = false
}
</script>

<template>
  <div class="discussion-editor" :class="{ 'is-focused': isFocused }">
    <div v-if="replyToUsername" class="reply-to-hint">
      回复 @{{ replyToUsername }}
    </div>
    <div class="editor-textarea-wrapper">
      <textarea
        :value="content"
        :placeholder="placeholder"
        :maxlength="maxLength + 1"
        :rows="isFocused ? 4 : 2"
        class="editor-textarea"
        :class="{ 'over-limit': isOverLimit, 'near-limit': isNearLimit && !isOverLimit }"
        @input="handleInput"
        @focus="handleFocus"
        @blur="handleBlur"
      />
    </div>
    <div class="editor-footer">
      <div class="char-count" :class="{ 'over-limit': isOverLimit, 'near-limit': isNearLimit && !isOverLimit }">
        {{ charCount }}/{{ maxLength }}
      </div>
      <div class="editor-actions">
        <el-button
          v-if="showCancel"
          size="small"
          @click="handleCancel"
          :disabled="loading"
        >
          取消
        </el-button>
        <el-button
          type="primary"
          size="small"
          :loading="loading"
          :disabled="!content.trim() || isOverLimit"
          @click="handleSubmit"
        >
          提交
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.discussion-editor {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-3);
  transition: all var(--motion-fast) var(--ease-standard);
}

.discussion-editor.is-focused {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-focus);
}

.reply-to-hint {
  font-size: 13px;
  color: var(--color-text-3);
  margin-bottom: var(--space-2);
  padding: 6px 12px;
  background: var(--color-primary-soft);
  border-radius: 8px;
  display: inline-block;
}

.reply-to-hint::before {
  content: '↳ ';
  color: var(--color-primary);
  font-weight: 600;
}

.editor-textarea-wrapper {
  width: 100%;
}

.editor-textarea {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-2);
  background: transparent;
  transition: all var(--motion-fast) var(--ease-standard);
}

.editor-textarea::placeholder {
  color: var(--color-text-3);
}

.editor-textarea.over-limit {
  color: var(--el-color-danger);
}

.editor-textarea.near-limit {
  color: var(--el-color-warning);
}

.editor-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-2);
  padding-top: var(--space-2);
  border-top: 1px solid var(--color-border);
}

.char-count {
  font-size: 12px;
  color: var(--color-text-3);
  font-weight: 500;
  transition: color var(--motion-fast) var(--ease-standard);
}

.char-count.near-limit {
  color: var(--el-color-warning);
}

.char-count.over-limit {
  color: var(--el-color-danger);
  font-weight: 600;
}

.editor-actions {
  display: flex;
  gap: var(--space-2);
}

@media (max-width: 768px) {
  .discussion-editor {
    padding: var(--space-2);
  }

  .editor-textarea {
    font-size: 14px;
  }
}
</style>
