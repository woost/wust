angular.module("wust.elements").controller("TagRelationCtrl", TagRelationCtrl);

TagRelationCtrl.$inject = ["$scope", "TagRelationEditService", "Auth", "KarmaService"];

function TagRelationCtrl($scope, TagRelationEditService, Auth, KarmaService) {
    let vm = this;

    vm.save = save;
    vm.cancel = cancel;
    vm.currentEditRelation = TagRelationEditService.currentRelation;
    vm.disconnect = TagRelationEditService.disconnect;

    let authorBoost = Auth.current.userId === vm.currentEditRelation.startNode.author.id ? wust.Moderation().authorKarmaBoost : 0;
    $scope.$on("component.changed", calculateEditWeight);
    $scope.$on("karma.changed", calculateEditWeight);
    calculateEditWeight();

    function calculateEditWeight() {
        vm.editWeight = KarmaService.voteWeightInContexts(vm.currentEditRelation.startNode.connectedTags) + authorBoost;
        vm.canApply = wust.Moderation().canApply(vm.editWeight, vm.currentEditRelation.startNode.postChangeThreshold);
    }

    function cancel() {
        if( vm.currentEditRelation.isCreating )
            vm.disconnect();
    }

    function save() {
        vm.currentEditRelation.save();
    }
}
