module Api
  class GraphsController < ApplicationController
    respond_to :json

    def show
      graph = {
        nodes: [
          1, 2, 3
        ],
        edges: [
          [1, 2],
          [2, 3]
        ]
      }

      render json: { graph: graph }, status: :ok
    end
  end
end
