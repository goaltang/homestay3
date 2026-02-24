<template>
    <component :is="iconName" v-bind="$attrs" />
</template>

<script setup>
import { defineComponent, h } from 'vue'

const props = defineProps({
    iconName: {
        type: String,
        required: true
    }
})

// 自定义SVG图标组件
const FamilyKitchen = defineComponent({
    name: 'FamilyKitchen',
    render() {
        return h('svg', {
            viewBox: '0 0 24 24',
            fill: 'currentColor',
            width: '1em',
            height: '1em'
        }, [
            h('path', {
                d: 'M18 2.01L6 2c-1.1 0-2 .89-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.11-.9-1.99-2-1.99zM18 20H6v-9.02h12V20z'
            }),
            h('circle', { cx: '8', cy: '15', r: '1' }),
            h('circle', { cx: '12', cy: '15', r: '1' }),
            h('circle', { cx: '16', cy: '15', r: '1' }),
            h('path', { d: 'M8 6h8v2H8z' })
        ])
    }
})

const ConvenientTransport = defineComponent({
    name: 'ConvenientTransport',
    render() {
        return h('svg', {
            viewBox: '0 0 24 24',
            fill: 'currentColor',
            width: '1em',
            height: '1em'
        }, [
            h('path', {
                d: 'M19 7c0-1.1-.9-2-2-2h-3V3c0-.55-.45-1-1-1h-2c-.55 0-1 .45-1 1v2H7c-1.1 0-2 .9-2 2v11h2c0 1.66 1.34 3 3 3s3-1.34 3-3h4c0 1.66 1.34 3 3 3s3-1.34 3-3h2V7zM10 19c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm8 0c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1z'
            }),
            h('path', { d: 'M2 12l3-3v2h4v2H5v2z' })
        ])
    }
})

const PetParadise = defineComponent({
    name: 'PetParadise',
    render() {
        return h('svg', {
            viewBox: '0 0 24 24',
            fill: 'currentColor',
            width: '1em',
            height: '1em'
        }, [
            h('circle', { cx: '4.5', cy: '9.5', r: '2.5' }),
            h('circle', { cx: '9', cy: '5.5', r: '2.5' }),
            h('circle', { cx: '15', cy: '5.5', r: '2.5' }),
            h('circle', { cx: '19.5', cy: '9.5', r: '2.5' }),
            h('path', {
                d: 'M17.34 14.86c-.87-1.02-1.6-1.89-2.48-2.91-.46-.54-1.05-1.08-1.75-1.32-.11-.04-.22-.07-.33-.09-.25-.04-.52-.04-.77-.01-.11.02-.22.05-.33.09-.7.24-1.29.78-1.75 1.32-.87 1.02-1.6 1.89-2.48 2.91-1.31 1.31-2.92 2.76-2.62 4.79.29 1.02 1.02 2.03 2.33 2.32.73.15 3.06-.44 5.54-.44s4.81.59 5.54.44c1.31-.29 2.04-1.3 2.33-2.32.3-2.03-1.31-3.48-2.62-4.79z'
            })
        ])
    }
})

const RemoteWork = defineComponent({
    name: 'RemoteWork',
    render() {
        return h('svg', {
            viewBox: '0 0 24 24',
            fill: 'currentColor',
            width: '1em',
            height: '1em'
        }, [
            h('path', {
                d: 'M20 3H4c-1.1 0-2 .9-2 2v11c0 1.1.9 2 2 2h3l-1 1v2h12v-2l-1-1h3c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 13H4V5h16v11z'
            }),
            h('path', { d: 'M6.5 7.5h3v1h-3zM6.5 9.5h5v1h-5zM6.5 11.5h4v1h-4z' }),
            h('circle', { cx: '16', cy: '10', r: '2' })
        ])
    }
})

const FullService = defineComponent({
    name: 'FullService',
    render() {
        return h('svg', {
            viewBox: '0 0 24 24',
            fill: 'currentColor',
            width: '1em',
            height: '1em'
        }, [
            h('path', {
                d: 'M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z'
            }),
            h('circle', { cx: '12', cy: '12', r: '3', fill: 'white' }),
            h('text', { x: '12', y: '16', textAnchor: 'middle', fontSize: '8', fill: 'currentColor' }, '服务')
        ])
    }
})

const ThoughtfulService = defineComponent({
    name: 'ThoughtfulService',
    render() {
        return h('svg', {
            viewBox: '0 0 24 24',
            fill: 'currentColor',
            width: '1em',
            height: '1em'
        }, [
            h('path', {
                d: 'M18.5 4l-1.16.73c-.12-.08-.25-.15-.39-.2l-.37-1.36c-.08-.27-.33-.47-.61-.47h-1.94c-.28 0-.53.2-.61.47L13.05 4.53c-.14.05-.27.12-.39.2L11.5 4c-.25-.16-.58-.1-.76.15L9.26 6.24c-.18.25-.14.59.08.78l1.16.73c0 .06-.01.12-.01.18v.14c0 .06.01.12.01.18l-1.16.73c-.22.19-.26.53-.08.78l1.48 2.09c.18.25.51.31.76.15l1.16-.73c.12.08.25.15.39.2l.37 1.36c.08.27.33.47.61.47h1.94c.28 0 .53-.2.61-.47l.37-1.36c.14-.05.27-.12.39-.2l1.16.73c.25.16.58.1.76-.15l1.48-2.09c.18-.25.14-.59-.08-.78L18.5 9.82c0-.06.01-.12.01-.18V9.5c0-.06-.01-.12-.01-.18L19.66 8.59c.22-.19.26-.53.08-.78L18.26 5.72c-.18-.25-.51-.31-.76-.15z'
            }),
            h('circle', { cx: '15', cy: '9', r: '2.5' }),
            h('path', { d: 'M2 17h20v2H2zM4 19h16v2H4z' })
        ])
    }
})

const EntertainmentHub = defineComponent({
    name: 'EntertainmentHub',
    render() {
        return h('svg', {
            viewBox: '0 0 24 24',
            fill: 'currentColor',
            width: '1em',
            height: '1em'
        }, [
            h('path', {
                d: 'M21 3H3c-1.11 0-2 .89-2 2v12c0 1.1.89 2 2 2h5v2h8v-2h5c1.1 0 1.99-.9 1.99-2L23 5c0-1.11-.9-2-2-2zm0 12H3V5h18v10z'
            }),
            h('path', { d: 'm9 8 5 3-5 3V8z' }),
            h('circle', { cx: '18', cy: '8', r: '1.5' }),
            h('circle', { cx: '18', cy: '12', r: '1.5' })
        ])
    }
})

const PriceAdvantage = defineComponent({
    name: 'PriceAdvantage',
    render() {
        return h('svg', {
            viewBox: '0 0 24 24',
            fill: 'currentColor',
            width: '1em',
            height: '1em'
        }, [
            h('path', {
                d: 'M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5 2.1h2.21c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91 3.46 4.7 4.13 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1h-2.2c.12 2.19 1.76 3.42 3.68 3.83V21h3v-2.15c1.95-.37 3.5-1.5 3.5-3.55 0-2.84-2.43-3.81-4.7-4.4z'
            })
        ])
    }
})

// 图标映射
const iconComponents = {
    FamilyKitchen,
    ConvenientTransport,
    PetParadise,
    RemoteWork,
    FullService,
    ThoughtfulService,
    EntertainmentHub,
    PriceAdvantage
}
</script>

<style scoped>
svg {
    display: inline-block;
    vertical-align: middle;
}
</style>