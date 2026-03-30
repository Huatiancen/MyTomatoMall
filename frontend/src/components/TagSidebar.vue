<template>
  <div class="tag-sidebar">
    <h3 class="sidebar-title">分类筛选</h3>
    <div
        v-for="(subTags, mainTag) in groupedTags"
        :key="mainTag"
        class="tag-group"
        @mouseenter="hovered = mainTag"
        @mouseleave="hovered = null"
    >
      <button
          class="main-tag"
          :class="{ 'active': activeTag === mainTag }"
          @click="selectTag(mainTag)"
      >
        {{ mainTag }}
      </button>

      <transition name="fade">
        <div v-if="hovered === mainTag" class="sub-tags">
          <button
              v-for="sub in subTags"
              :key="sub"
              class="sub-tag"
              :class="{ 'active': activeTag === mainTag && activeSubTag === sub }"
              @click="selectTag(mainTag, sub)"
          >
            {{ sub }}
          </button>
        </div>
      </transition>
    </div>
  </div>
</template>

<script>
import tagsData from "../assets/tags.json";

export default {
  name: "TagSidebar",
  data() {
    return {
      tags: tagsData,
      hovered: null,
      activeTag: null,
      activeSubTag: null
    };
  },
  computed: {
    groupedTags() {
      const map = {};
      this.tags.forEach(([main, sub]) => {
        if (!map[main]) map[main] = [];
        map[main].push(sub);
      });
      return map;
    }
  },
  methods: {
    selectTag(main, sub = null) {
      // 如果点击的是子标签
      if (sub) {
        // 判断是否已经选中了该子标签，如果是，则只取消子标签选择
        if (this.activeTag === main && this.activeSubTag === sub) {
          this.activeSubTag = null;  // 只取消子标签选择状态
          this.$emit("filter", [main]); // 仍然使用主标签进行筛选
        } else {
          // 如果不是，设置为激活状态
          this.activeTag = main;
          this.activeSubTag = sub;
          this.$emit("filter", [main, sub]);
        }
        return;
      }

      // 点击主标签时重置子标签状态
      this.activeSubTag = null;

      // 点击的是主标签：如果已经是激活状态，则取消选择
      if (this.activeTag === main) {
        this.activeTag = null;
        this.$emit("filter", []); // 发送空查询，表示取消筛选
      } else {
        // 否则将其设为激活状态
        this.activeTag = main;
        this.$emit("filter", [main]);
      }
    }
  }
};
</script>

<style scoped>
.tag-sidebar {
  width: 140px;
  height: calc(100vh - 60px);
  padding: 15px 10px;
  background-color: #f8f8f8;
  border-right: 1px solid #e0e0e0;
  box-shadow: 2px 0 6px rgba(0,0,0,0.05);
  position: fixed;
  top: 60px;
  left: 0;
  z-index: 900;
  /* 移除 overflow-y: auto 属性，避免截断子菜单 */
  overflow: visible; /* 允许元素溢出，不裁剪 */
}

/* 单独创建可滚动内容区域 */
.sidebar-content {
  height: 100%;
  overflow-y: auto;
  scrollbar-width: thin;
}

/* 美化滚动条 */
.tag-sidebar::-webkit-scrollbar {
  width: 4px;
}

.tag-sidebar::-webkit-scrollbar-thumb {
  background: #d0d0d0;
  border-radius: 4px;
}

.sidebar-title {
  margin-top: 0;
  margin-bottom: 20px;
  padding-bottom: 10px;
  font-size: 16px;
  color: #333;
  border-bottom: 1px solid #e0e0e0;
  text-align: center;
  font-weight: 600;
}

.tag-group {
  position: relative;
  margin-bottom: 12px;
}

.main-tag {
  width: 100%;
  background: none;
  border: none;
  padding: 8px 10px;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 添加小箭头指示 */
.main-tag::after {
  content: '›';
  font-size: 18px;
  opacity: 0.6;
}

.main-tag:hover {
  background-color: #ececec;
}

.main-tag.active {
  background-color: #e74c3c;
  color: white;
  outline: none;         /* 移除焦点轮廓 */
  border: none;          /* 确保没有边框 */
}

.main-tag:focus {
  outline: none;
}

.main-tag.active::after {
  opacity: 1;
}

.sub-tag:focus {
  outline: none;
}

.sub-tag.active {
  color: #e74c3c;
  font-weight: bold;
  text-decoration: underline;
  outline: none;
  border: none;
  box-shadow: none;
}

.sub-tags {
  position: absolute;
  top: -5px;
  left: 140px;
  background: white;
  border: 1px solid #eee;
  border-radius: 4px;
  padding: 12px;
  box-shadow: 2px 2px 12px rgba(0,0,0,0.12);
  z-index: 999;
  min-width: 200px;
  max-width: 320px; /* 增加宽度以容纳多列 */
  display: flex; /* 改为弹性布局 */
  flex-wrap: wrap; /* 允许换行 */
  gap: 6px; /* 按钮之间的间距 */
}

/* 修改子标签按钮为内联元素 */
.sub-tag {
  display: inline-flex; /* 改为内联弹性布局 */
  align-items: center;
  background: none;
  border: none;
  padding: 3px 8px;
  margin: 0;
  text-align: center;
  cursor: pointer;
  border-radius: 2px;
  font-size: 13px;
  transition: all 0.15s;
  color: #606266; /* 文本风格 */
  font-weight: normal;
  white-space: nowrap; /* 防止文本换行 */
}

.sub-tag:hover {
  color: #e74c3c;
  text-decoration: underline; /* 添加下划线效果 */
}

/* 更平滑的过渡动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.25s, transform 0.25s;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

.fade-enter, .fade-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}
</style>