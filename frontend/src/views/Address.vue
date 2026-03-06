<template>
  <div class="address">
    <h2>收货地址管理</h2>
    <el-button type="primary" @click="showDialog = true" style="margin-bottom: 20px;">添加地址</el-button>
    
    <el-table :data="addressList" v-loading="loading" style="width: 100%">
      <el-table-column prop="receiverName" label="收货人" width="120"></el-table-column>
      <el-table-column prop="receiverPhone" label="手机号" width="150"></el-table-column>
      <el-table-column label="地址" prop="fullAddress">
        <template slot-scope="scope">
          {{ scope.row.province }}{{ scope.row.city }}{{ scope.row.district }}{{ scope.row.detailAddress }}
        </template>
      </el-table-column>
      <el-table-column prop="isDefault" label="默认地址" width="100">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.isDefault === 1" type="success">是</el-tag>
          <span v-else>否</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template slot-scope="scope">
          <el-button size="small" @click="editAddress(scope.row)">编辑</el-button>
          <el-button type="danger" size="small" @click="deleteAddress(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-dialog :title="dialogTitle" :visible.sync="showDialog" width="600px">
      <el-form :model="addressForm" :rules="rules" ref="addressForm" label-width="100px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="addressForm.receiverName"></el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="addressForm.receiverPhone"></el-input>
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="addressForm.province"></el-input>
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="addressForm.city"></el-input>
        </el-form-item>
        <el-form-item label="区县" prop="district">
          <el-input v-model="addressForm.district"></el-input>
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input type="textarea" v-model="addressForm.detailAddress"></el-input>
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="addressForm.isDefault" :active-value="1" :inactive-value="0"></el-switch>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAddress">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { addressApi } from '../api'

export default {
  name: 'Address',
  data() {
    return {
      addressList: [],
      loading: false,
      showDialog: false,
      addressForm: {
        id: null,
        receiverName: '',
        receiverPhone: '',
        province: '',
        city: '',
        district: '',
        detailAddress: '',
        isDefault: 0
      },
      rules: {
        receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
        receiverPhone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
        province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
        city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
        district: [{ required: true, message: '请输入区县', trigger: 'blur' }],
        detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.addressForm.id ? '编辑地址' : '添加地址'
    }
  },
  created() {
    this.loadAddresses()
  },
  methods: {
    async loadAddresses() {
      this.loading = true
      try {
        const res = await addressApi.getAddressList()
        this.addressList = res.data
      } catch (error) {
        this.$message.error('加载地址失败')
      } finally {
        this.loading = false
      }
    },
    editAddress(address) {
      this.addressForm = { ...address }
      this.showDialog = true
    },
    async saveAddress() {
      this.$refs.addressForm.validate(async (valid) => {
        if (valid) {
          try {
            if (this.addressForm.id) {
              await addressApi.updateAddress(this.addressForm)
              this.$message.success('更新成功')
            } else {
              await addressApi.addAddress(this.addressForm)
              this.$message.success('添加成功')
            }
            this.showDialog = false
            this.resetForm()
            this.loadAddresses()
          } catch (error) {
            this.$message.error(error.message || '操作失败')
          }
        }
      })
    },
    async deleteAddress(id) {
      try {
        await this.$confirm('确定要删除该地址吗？', '提示', {
          type: 'warning'
        })
        await addressApi.deleteAddress(id)
        this.$message.success('删除成功')
        this.loadAddresses()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.message || '删除失败')
        }
      }
    },
    resetForm() {
      this.addressForm = {
        id: null,
        receiverName: '',
        receiverPhone: '',
        province: '',
        city: '',
        district: '',
        detailAddress: '',
        isDefault: 0
      }
      if (this.$refs.addressForm) {
        this.$refs.addressForm.resetFields()
      }
    }
  },
  watch: {
    showDialog(val) {
      if (!val) {
        this.resetForm()
      }
    }
  }
}
</script>

<style scoped>
.address {
  padding: 20px;
}
</style>
