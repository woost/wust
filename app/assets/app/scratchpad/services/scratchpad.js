angular.module("wust.scratchpad").service("Scratchpad", Scratchpad);

Scratchpad.$inject = ["DiscourseNode"];

function Scratchpad(DiscourseNode) {
    this.settings = {
        visible: false
    };
}
