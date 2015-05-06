angular.module("wust.components").controller("FooterCtrl", FooterCtrl);

FooterCtrl.$inject = ["NodeHistory"];

function FooterCtrl(NodeHistory) {
    let vm = this;
    vm.visited = NodeHistory.visited;
}
