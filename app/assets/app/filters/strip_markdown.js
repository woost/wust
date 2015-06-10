angular.module("wust.filters").filter("stripMarkdown", stripMarkdown);

stripMarkdown.$inject = [];

function stripMarkdown() {
  return text => text.replace(/`|#|>/g, "");
}
