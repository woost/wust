angular.module("wust.services").service("EditPopoverService", EditPopoverService);

EditPopoverService.$inject = ["EditService"];

function EditPopoverService(EditService) {
    let editNode;

    Object.defineProperty(this, "editNode", {
        get: () => editNode,
        set: val => {
            editNode = val;
            EditService.edit(editNode);
        }
    });
}
