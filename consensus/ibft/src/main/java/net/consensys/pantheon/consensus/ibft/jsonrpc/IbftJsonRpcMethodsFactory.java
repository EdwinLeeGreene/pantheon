package net.consensys.pantheon.consensus.ibft.jsonrpc;

import net.consensys.pantheon.consensus.ibft.IbftContext;
import net.consensys.pantheon.consensus.ibft.jsonrpc.methods.IbftProposeValidatorVote;
import net.consensys.pantheon.ethereum.ProtocolContext;
import net.consensys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import net.consensys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;

import java.util.HashMap;
import java.util.Map;

public class IbftJsonRpcMethodsFactory {

  private final JsonRpcParameter jsonRpcParameter = new JsonRpcParameter();

  public Map<String, JsonRpcMethod> methods(final ProtocolContext<IbftContext> context) {

    final Map<String, JsonRpcMethod> rpcMethods = new HashMap<>();
    // @formatter:off
    addMethods(
        rpcMethods,
        new IbftProposeValidatorVote(
            context.getConsensusState().getVoteProposer(), jsonRpcParameter));

    return rpcMethods;
  }

  private void addMethods(
      final Map<String, JsonRpcMethod> methods, final JsonRpcMethod... rpcMethods) {
    for (JsonRpcMethod rpcMethod : rpcMethods) {
      methods.put(rpcMethod.getName(), rpcMethod);
    }
  }
}