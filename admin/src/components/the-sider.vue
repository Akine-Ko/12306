<template>
  <a-layout-sider width="200" style="background: #fff">
    <a-menu
        v-model:selectedKeys="selectedKeys"
        mode="inline"
        :style="{ height: '100%', borderRight:0}"
    >
      <a-menu-item key="/welcome">
        <router-link to="/welcome">
          <coffee-outlined/> &nbsp; 欢迎
        </router-link>
      </a-menu-item>

      <a-menu-item key="/about">
        <router-link to="/about">
          <user-outlined /> &nbsp; 关于
        </router-link>
      </a-menu-item>

      <a-menu-item key="/station">
        <router-link to="/station">
          <user-outlined /> &nbsp; &nbsp; 车站管理
        </router-link>
      </a-menu-item>

      <a-menu-item key="/train">
        <router-link to="/train">
          <user-outlined /> &nbsp; &nbsp; 火车管理
        </router-link>
      </a-menu-item>

      <a-menu-item key="/train-station">
        <router-link to="/train-station">
          <user-outlined /> &nbsp; &nbsp; 火车车站
        </router-link>
      </a-menu-item>

      <a-menu-item key="/train-carriage">
        <router-link to="/train-carriage">
          <user-outlined /> &nbsp; &nbsp; 火车车厢
        </router-link>
      </a-menu-item>

      <a-menu-item key="/train-seat">
        <router-link to="/train-seat">
          <user-outlined /> &nbsp; &nbsp; 火车座位
        </router-link>
      </a-menu-item>
    </a-menu>
  </a-layout-sider>
</template>

<script>
import {defineComponent, ref, watch} from 'vue';
import router from '@/router';


export default defineComponent({
  name: "the-sider-view",
  setup() {
    const selectedKeys = ref([]);

    watch(() => router.currentRoute.value.path, (newValue) => {
      console.log('watch', newValue);
      selectedKeys.value = [];
      selectedKeys.value.push(newValue);
    }, {immediate: true});
    return {
      selectedKeys,
    };
  },
});
</script>

<style scoped>


</style>