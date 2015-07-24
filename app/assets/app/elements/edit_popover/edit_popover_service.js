angular.module("wust.services").service("EditPopoverService", EditPopoverService);

EditPopoverService.$inject = ["EditService"];

function EditPopoverService(EditService) {
    let currentEditNode;

    Object.defineProperty(this, "editNode", {
        get: () => currentEditNode,
        set: val => {
            currentEditNode = EditService.edit(val);
        }
    });
}
