angular.module("wust").filter("stripMarkdown", stripMarkdown);

stripMarkdown.$inject = [];

function stripMarkdown() {
  return text => text.replace(/`|#|>/g, "");
}
