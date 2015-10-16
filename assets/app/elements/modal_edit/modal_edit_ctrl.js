angular.module("wust.elements").controller("ModalEditCtrl", ModalEditCtrl);

ModalEditCtrl.$inject = ["$scope", "DiscourseNode", "Search", "EditService", "ModalEditService", "Auth", "KarmaService"];

function ModalEditCtrl($scope, DiscourseNode, Search, EditService, ModalEditService, Auth, KarmaService) {
    let vm = this;


    vm.hasFocus = true;
    vm.previewEnabled = false;
    vm.save = save;
    vm.node = ModalEditService.currentNode;
    //TODO: editsession should know this stuff
    vm.editedComponentNode = ModalEditService.editedComponentNode;

    let authorBoost;
    if (vm.editedComponentNode) {
        authorBoost = Auth.current.userId === vm.editedComponentNode.author.id ? wust.Moderation().authorKarmaBoost : 0;
        $scope.$on("component.changed", calculateEditWeight);
        $scope.$on("karma.changed", calculateEditWeight);
        calculateEditWeight();
    }

    function save() {
        ModalEditService.save();
    }

    function calculateEditWeight() {
        vm.editWeight = KarmaService.voteWeightInContexts(vm.editedComponentNode.connectedTags) + authorBoost;
        vm.canApply = wust.Moderation().canApply(vm.editWeight, vm.editedComponentNode.postChangeThreshold);
    }
}
