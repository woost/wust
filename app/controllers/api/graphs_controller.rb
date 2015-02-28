module Api
  class GraphsController < ApplicationController
    respond_to :json

    def show
      graph = Graph.problem_graph(params[:id].to_i)
      if graph
        render json: { graph:  graph }, status: :ok
      else
        render json: {}, status: :unprocessable_entity
      end
    end

    def index
      render json: { graph: Graph.problem_graphs }, status: :ok
    end
  end
end
