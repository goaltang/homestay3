import { defineStore } from "pinia";

export const useSidebarStore = defineStore("sidebar", {
  state: () => {
    return {
      collapse: false,
      bgColor: "#242f42",
      textColor: "#fff",
    };
  },
  actions: {
    handleCollapse() {
      this.collapse = !this.collapse;
    },
  },
});
