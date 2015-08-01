angular.module("wust.services").service("ModalEditService", ModalEditService);

ModalEditService.$inject = ["$rootScope", "$modal", "EditService"];

function ModalEditService($rootScope, $modal, EditService) {
    let self = this;

    let modalInstance = $modal({
        show: false,
        templateUrl: "assets/app/elements/modal_edit/modal_edit.html",
        controller: "ModalEditCtrl",
        controllerAs: "vm",
        animation: "am-fade-and-slide-top"
    });

    this.show = showModal;
    this.hide = hideModal;
    this.save = save;
    this.onSave = onSave;
    // FIXME:
    // 1. we need to do this per session in the editservice, as the savehandler and the reference may not be lost
    // 2. we should not support savehandlers anyhow. the only thing you want to achieve is answering a node
    // 3. how can we serialize such a reference? after page reload the connection to the graph is lost...
    // 4. it does not show on the first modal...why? i do not know.
    this.reference = {
    };

    let currentNode;
    //TODO: this does not work when the browser is reloaded and also it might be that we have removed the session from the scratchpad
    Object.defineProperty(this, "currentNode", {
        get: () => {
            if (currentNode === undefined || !currentNode.isLocal) {
                self.reference.saveHandler = undefined;
                self.reference.node = undefined;
                currentNode = EditService.edit();
                return currentNode;
            } else {
                return currentNode;
            }
        }
    });

    function onSave(node, saveHandler) {
        node = node.encode ? node.encode() : node;
        self.reference.saveHandler = saveHandler;
        self.reference.node = node;
        //TODO: avoid
        $rootScope.$apply();
    }

    function save() {
        if (currentNode === undefined)
            return;

        if (self.reference.saveHandler !== undefined) {
            currentNode.save().$then(data => {
                self.reference.saveHandler(data);
                self.reference.saveHandler = undefined;
            });
        }
    }

    function showModal() {
        modalInstance.$promise.then(modalInstance.show);
    }

    function hideModal() {
        modalInstance.$promise.then(modalInstance.hide);
    }
}

