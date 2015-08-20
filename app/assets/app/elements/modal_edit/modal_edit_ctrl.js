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
            Search.$search({
                title: "RepliesTo",
                label: DiscourseNode.TagLike.label,
                size: 1,
                page: 0
            }).$then(val => {
                //TODO: get created relation
                let createdRel = _.find(response.graph.nodes, n => n.isHyperRelation && n.startId === vm.node.id && n.endId === ref.id);
                let edited = EditService.createSession(createdRel);
                edited.tags.push(val[0]);
                edited.save();
            });
        });
    }
}
