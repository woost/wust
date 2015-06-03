angular.module("wust.components").controller("FocusCtrl", FocusCtrl);

FocusCtrl.$inject = ["$stateParams", "component"];

function FocusCtrl($stateParams, component) {
    let vm = this;

    vm.id = $stateParams.id;
    vm.component = component;
}
