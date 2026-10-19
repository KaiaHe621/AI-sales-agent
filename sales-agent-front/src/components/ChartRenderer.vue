<template>
  <div class="chart-wrapper">
    <div ref="chartEl" class="chart-canvas"></div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: {
    type: Object,
    required: true,
  },
})

const chartEl = ref(null)
let chart = null

const defaultOption = {
  backgroundColor: 'transparent',
  textStyle: { fontFamily: 'inherit', color: '#334155' },
  grid: { top: 40, right: 20, bottom: 40, left: 50, containLabel: true },
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(15,23,42,0.9)',
    borderColor: 'rgba(255,255,255,0.1)',
    textStyle: { color: '#e2e8f0', fontSize: 13 },
    extraCssText: 'border-radius:10px;padding:10px 14px;box-shadow:0 8px 32px rgba(0,0,0,0.3)',
  },
  color: ['#4F46E5', '#06B6D4', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6'],
}

function initChart() {
  if (!chartEl.value) return
  chart = echarts.init(chartEl.value, null, { renderer: 'canvas' })

  const merged = mergeOption(defaultOption, props.option)
  chart.setOption(merged)
}

function mergeOption(base, override) {
  const result = { ...base, ...override }
  if (base.textStyle && override.textStyle) {
    result.textStyle = { ...base.textStyle, ...override.textStyle }
  }
  if (base.tooltip && override.tooltip) {
    result.tooltip = { ...base.tooltip, ...override.tooltip }
  }
  if (!override.grid) {
    result.grid = base.grid
  }
  return result
}

const ro = new ResizeObserver(() => chart?.resize())

onMounted(() => {
  initChart()
  if (chartEl.value) ro.observe(chartEl.value)
})

onUnmounted(() => {
  ro.disconnect()
  chart?.dispose()
})

watch(
  () => props.option,
  (newOpt) => {
    if (chart) {
      const merged = mergeOption(defaultOption, newOpt)
      chart.setOption(merged, true)
    }
  },
  { deep: true }
)
</script>

<style scoped>
.chart-wrapper {
  width: 100%;
  margin-top: 12px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--border);
  background: var(--bg-card);
  box-shadow: var(--shadow-sm);
}

.chart-canvas {
  width: 100%;
  height: 380px;
}
</style>
