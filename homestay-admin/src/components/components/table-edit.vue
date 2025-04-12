<template>
	<el-form ref="formRef" :model="form" :rules="rules" :label-width="options.labelWidth">
		<el-row>
			<el-col :span="options.span" v-for="item in dynamicOptions" :key="item.prop">
				<el-form-item :label="item.label" :prop="item.prop">
					<!-- 文本框、数字框、下拉框、日期框、开关、上传 -->
					<el-input v-if="item.type === 'input'" v-model="form[item.prop]" :disabled="item.disabled"
						:placeholder="item.placeholder" clearable></el-input>
					<el-input-number v-else-if="item.type === 'number'" v-model="form[item.prop]"
						:disabled="item.disabled" controls-position="right"></el-input-number>
					<el-select v-else-if="item.type === 'select'" v-model="form[item.prop]" :disabled="item.disabled"
						:placeholder="item.placeholder" clearable>
						<el-option v-for="opt in item.opts" :key="opt.value" :label="opt.label"
							:value="opt.value"></el-option>
					</el-select>
					<el-date-picker v-else-if="item.type === 'date'" type="datetime" v-model="form[item.prop]"
						:value-format="item.format"></el-date-picker>
					<el-switch v-else-if="item.type === 'switch'" v-model="form[item.prop]"
						:active-value="item.activeValue" :inactive-value="item.inactiveValue"
						:active-text="item.activeText" :inactive-text="item.inactiveText"
						@change="handleSwitchChange"></el-switch>
					<el-upload v-else-if="item.type === 'upload'" class="avatar-uploader" action="#"
						:show-file-list="false" :on-success="handleAvatarSuccess">
						<img v-if="form[item.prop]" :src="form[item.prop]" class="avatar" />
						<el-icon v-else class="avatar-uploader-icon">
							<Plus />
						</el-icon>
					</el-upload>
					<slot :name="item.prop" v-else></slot>
				</el-form-item>
			</el-col>
		</el-row>

		<el-form-item>
			<el-button type="primary" @click="save(formRef)">保 存</el-button>
		</el-form-item>
	</el-form>
</template>

<script lang="ts" setup>
import { FormOption } from '@/types/form-option';
import { FormInstance, FormRules, UploadProps } from 'element-plus';
import { PropType, ref, watch } from 'vue';

const { options, formData, edit, update, add, animeOperator } = defineProps({
	options: {
		type: Object as PropType<FormOption>,
		required: true
	},
	formData: {
		type: Object,
		required: true
	},
	edit: {
		type: Boolean,
		required: false
	},
	update: {
		type: Function,
		required: true
	},
	add: {
		type: Function,
		required: true
	},
	animeOperator: {
		type: Boolean,
		required: false
	}
});

// 定义 rowData
const rowData = ref({ ...formData });

// const form = ref({ ...(edit ? formData : {}) });

//使用 watch 监听 edit 的变化，来动态调整表单字段
// 根据编辑状态初始化表单数据的功能
const form = ref<Partial<typeof formData>>({ ...(edit || animeOperator ? formData : {}) });

// 动态修改表单字段的逻辑，根据是否是编辑模式调整 options
const dynamicOptions = ref(options.list);

// 如果是编辑模式，则显示 ID，如果是新增模式则隐藏 ID
watch(() => edit, (newValue) => {
	if (newValue) {
		dynamicOptions.value = [...options.list];
	} else {
		// 在新增模式下通过 filter 移除 id 字段，确保只显示板块名称。
		dynamicOptions.value = options.list.filter(item => item.prop !== 'id');
	}
}, { immediate: true });  // 确保组件加载时立即触发);

//表单生成的规则 rules 根据 edit 的值动态调整，确保字段验证准确
const rules: FormRules = dynamicOptions.value.length > 0 ?
	dynamicOptions.value.map(item => {
		if (item.required) {
			return { [item.prop]: [{ required: true, message: `${item.label}不能为空`, trigger: 'blur' }] };
		}
		return {};
	}).reduce((acc, cur) => ({ ...acc, ...cur }), {}) : {};

const formRef = ref<FormInstance>();
const save = (formEl: FormInstance | undefined) => {
	console.log(`保存按钮调用`)
	if (!formEl) return;

	// 验证表单内容
	formEl.validate((valid: boolean) => {
		console.log(`验证结果:`, valid);  // 调试日志
		console.log(`form值:`, form.value);  // 调试日志

		if (!valid) return false;

		// 根据是否是审核页面判断操作
		if (animeOperator) {
			// 动漫审核页逻辑
			update(form);
		} else {
			// 编辑页逻辑
			console.log(`不是动漫页`)
			if (edit) {
				// 编辑模式，调用更新接口
				console.log(`是编辑页`)
				update(form);
			} else {
				// 新增模式，调用新增接口
				console.log(`是新增页`)
				add(form);
			}
		}
	});
};




const handleAvatarSuccess: UploadProps['onSuccess'] = (response, uploadFile) => {
	form.value.thumb = URL.createObjectURL(uploadFile.raw!);
};

const handleSwitchChange = (value: boolean) => {
	console.log('开关切换到:', value);
	// form.value.process = value;  // 同步开关状态
};

</script>

<style>
.avatar-uploader .el-upload {
	border: 1px dashed var(--el-border-color);
	border-radius: 6px;
	cursor: pointer;
	position: relative;
	overflow: hidden;
	transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
	border-color: var(--el-color-primary);
}

.el-icon.avatar-uploader-icon {
	font-size: 28px;
	color: #8c939d;
	width: 178px;
	height: 178px;
	text-align: center;
}
</style>
