module Api
  class GraphsController < ApplicationController
    respond_to :json

    def show
      nodes = Graph.problem_nodes(params[:id].to_i)
      if nodes
        render json: nodes, status: :ok
      else
        render json: {}, status: :unprocessable_entity
      end
    end

    def index
      render json: Graph.problem_graph, status: :ok
    end
  end
end
