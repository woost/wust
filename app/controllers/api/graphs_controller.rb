module Api
  class GraphsController < ApplicationController
    respond_to :json

    def show
      graph = Graph.graph(params[:id].to_i)
      if graph
        render json: graph, status: :ok
      else
        render json: {}, status: :unprocessable_entity
      end
    end

    def index
      render json: Graph.problem_graph, status: :ok
    end
  end
end
