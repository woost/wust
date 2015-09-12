angular.module("wust.elements").controller("ModalEditCtrl", ModalEditCtrl);

ModalEditCtrl.$inject = ["DiscourseNode", "Search", "EditService", "ModalEditService"];

function ModalEditCtrl(DiscourseNode, Search, EditService,ModalEditService) {
    let vm = this;

    vm.hasFocus = true;
    vm.previewEnabled = false;
    vm.node = ModalEditService.currentNode;

    vm.save = save;

    function save() {
        let ref = vm.node.referenceNode;
        return ModalEditService.save(response => {
            //TODO: get created relation
            let createdRel = _.find(response.graph.nodes, n => n.isHyperRelation && n.startId === vm.node.id && n.endId === ref.id);
            let edited = EditService.createSession(createdRel);
            edited.save();
        });
    }
}
