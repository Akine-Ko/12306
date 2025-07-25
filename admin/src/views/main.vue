<template>
  <a-layout id="components-layout-demo-top-side-2" style="height: 100vh">
    <the-header-view/>
    <a-layout>
      <the-sider-view/>
      <a-layout-content
        :style="{ background: '#fff', padding: '24px', margin: 0, minHeight: '280px' }"
      >
        <router-view/>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script>
import {defineComponent} from 'vue';
import TheHeaderView from "@/components/the-header.vue";
import TheSiderView from "@/components/the-sider.vue";

export default defineComponent({
  components: {
    TheSiderView,
    TheHeaderView
  },
  setup() {
    return {
    };
  },
});
</script>


<style>

</style>