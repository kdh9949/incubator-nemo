/*
 * Copyright (C) 2017 Seoul National University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.snu.nemo.runtime.master.physicalplans;

import edu.snu.nemo.common.coder.Coder;
import edu.snu.nemo.common.dag.DAG;
import edu.snu.nemo.common.dag.DAGBuilder;
import edu.snu.nemo.common.ir.edge.IREdge;
import edu.snu.nemo.common.ir.edge.executionproperty.DataCommunicationPatternProperty;
import edu.snu.nemo.common.ir.vertex.IRVertex;
import edu.snu.nemo.common.ir.vertex.OperatorVertex;
import edu.snu.nemo.common.ir.vertex.executionproperty.ExecutorPlacementProperty;
import edu.snu.nemo.common.ir.vertex.executionproperty.ParallelismProperty;
import edu.snu.nemo.common.ir.vertex.transform.Transform;
import edu.snu.nemo.compiler.optimizer.CompiletimeOptimizer;
import edu.snu.nemo.compiler.optimizer.examples.EmptyComponents;
import edu.snu.nemo.compiler.optimizer.policy.Policy;
import edu.snu.nemo.conf.JobConf;
import edu.snu.nemo.runtime.common.plan.physical.PhysicalPlan;
import edu.snu.nemo.runtime.common.plan.physical.PhysicalPlanGenerator;
import edu.snu.nemo.runtime.common.plan.physical.PhysicalStage;
import edu.snu.nemo.runtime.common.plan.physical.PhysicalStageEdge;
import org.apache.reef.tang.Injector;
import org.apache.reef.tang.Tang;
import org.apache.reef.tang.exceptions.InjectionException;

/**
 * Generates physical plans from IRs.
 */
public final class TestPlanGenerator {
  private static final PhysicalPlanGenerator PLAN_GENERATOR;
  private static final String EMPTY_DAG_DIRECTORY = "";

  static {
    final Injector injector = Tang.Factory.getTang().newInjector();
    injector.bindVolatileParameter(JobConf.DAGDirectory.class, EMPTY_DAG_DIRECTORY);
    try {
      PLAN_GENERATOR = injector.getInstance(PhysicalPlanGenerator.class);
    } catch (InjectionException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * private constructor.
   */
  private TestPlanGenerator() {
  }

  /**
   * @return push-based plan
   * @throws Exception exception
   */
  public static PhysicalPlan getSimplePushPlan() throws Exception {
    return convertIRToPhysical(buildSimpleIRDAG(), new BasicPushPolicy());
  }

  /**
   * @return pull-based plan
   * @throws Exception exception
   */
  public static PhysicalPlan getSimplePullPlan() throws Exception {
    return convertIRToPhysical(buildSimpleIRDAG(), new BasicPullPolicy());
  }

  /**
   * @param irDAG irDAG
   * @param policy policy
   * @return convert an IR into a physical plan using the given policy
   * @throws Exception exception
   */
  public static PhysicalPlan convertIRToPhysical(final DAG<IRVertex, IREdge> irDAG,
                                                  final Policy policy) throws Exception {
    final DAG<IRVertex, IREdge> optimized = CompiletimeOptimizer.optimize(irDAG, policy, EMPTY_DAG_DIRECTORY);
    final DAG<PhysicalStage, PhysicalStageEdge> physicalDAG = optimized.convert(PLAN_GENERATOR);
    return new PhysicalPlan("Plan", physicalDAG, PLAN_GENERATOR.getTaskIRVertexMap());
  }

  /**
   * @return a simple IR DAG
   */
  private static DAG<IRVertex, IREdge> buildSimpleIRDAG() {
    final DAGBuilder<IRVertex, IREdge> dagBuilder = new DAGBuilder<>();

    final Transform t = new EmptyComponents.EmptyTransform("empty");
    final IRVertex v1 = new OperatorVertex(t);
    v1.setProperty(ParallelismProperty.of(3));
    v1.setProperty(ExecutorPlacementProperty.of(ExecutorPlacementProperty.COMPUTE));
    dagBuilder.addVertex(v1);

    final IRVertex v2 = new OperatorVertex(t);
    v2.setProperty(ParallelismProperty.of(2));
    v2.setProperty(ExecutorPlacementProperty.of(ExecutorPlacementProperty.COMPUTE));
    dagBuilder.addVertex(v2);

    final IRVertex v3 = new OperatorVertex(t);
    v3.setProperty(ParallelismProperty.of(3));
    v3.setProperty(ExecutorPlacementProperty.of(ExecutorPlacementProperty.COMPUTE));
    dagBuilder.addVertex(v3);

    final IRVertex v4 = new OperatorVertex(t);
    v4.setProperty(ParallelismProperty.of(2));
    v4.setProperty(ExecutorPlacementProperty.of(ExecutorPlacementProperty.COMPUTE));
    dagBuilder.addVertex(v4);

    final IRVertex v5 = new OperatorVertex(t);
    v5.setProperty(ParallelismProperty.of(2));
    v5.setProperty(ExecutorPlacementProperty.of(ExecutorPlacementProperty.COMPUTE));
    dagBuilder.addVertex(v5);

    final IREdge e1 = new IREdge(DataCommunicationPatternProperty.Value.Shuffle, v1, v2, Coder.DUMMY_CODER);
    dagBuilder.connectVertices(e1);

    final IREdge e2 = new IREdge(DataCommunicationPatternProperty.Value.Shuffle, v3, v2, Coder.DUMMY_CODER);
    dagBuilder.connectVertices(e2);

    final IREdge e3 = new IREdge(DataCommunicationPatternProperty.Value.Shuffle, v2, v4, Coder.DUMMY_CODER);
    dagBuilder.connectVertices(e3);

    final IREdge e4 = new IREdge(DataCommunicationPatternProperty.Value.OneToOne, v4, v5, Coder.DUMMY_CODER);
    dagBuilder.connectVertices(e4);

    return dagBuilder.buildWithoutSourceSinkCheck();
  }
}

