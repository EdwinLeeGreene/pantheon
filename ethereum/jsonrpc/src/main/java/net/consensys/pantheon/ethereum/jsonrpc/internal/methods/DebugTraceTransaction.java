package net.consensys.pantheon.ethereum.jsonrpc.internal.methods;

import net.consensys.pantheon.ethereum.core.Hash;
import net.consensys.pantheon.ethereum.debug.TraceOptions;
import net.consensys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import net.consensys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import net.consensys.pantheon.ethereum.jsonrpc.internal.processor.TransactionTraceParams;
import net.consensys.pantheon.ethereum.jsonrpc.internal.processor.TransactionTracer;
import net.consensys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;
import net.consensys.pantheon.ethereum.jsonrpc.internal.queries.TransactionWithMetadata;
import net.consensys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import net.consensys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import net.consensys.pantheon.ethereum.jsonrpc.internal.results.DebugTraceTransactionResult;
import net.consensys.pantheon.ethereum.vm.DebugOperationTracer;

import java.util.Optional;

public class DebugTraceTransaction implements JsonRpcMethod {

  private final JsonRpcParameter parameters;
  private final TransactionTracer transactionTracer;
  private final BlockchainQueries blockchain;

  public DebugTraceTransaction(
      final BlockchainQueries blockchain,
      final TransactionTracer transactionTracer,
      final JsonRpcParameter parameters) {
    this.blockchain = blockchain;
    this.transactionTracer = transactionTracer;
    this.parameters = parameters;
  }

  @Override
  public String getName() {
    return "debug_traceTransaction";
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest request) {
    final Hash hash = parameters.required(request.getParams(), 0, Hash.class);
    final Optional<TransactionTraceParams> transactionTraceParams =
        parameters.optional(request.getParams(), 1, TransactionTraceParams.class);
    final Optional<TransactionWithMetadata> transactionWithMetadata =
        blockchain.transactionByHash(hash);
    final Hash blockHash = transactionWithMetadata.get().getBlockHash();
    final TraceOptions traceOptions =
        transactionTraceParams
            .map(TransactionTraceParams::traceOptions)
            .orElse(TraceOptions.DEFAULT);

    final DebugOperationTracer execTracer = new DebugOperationTracer(traceOptions);

    final DebugTraceTransactionResult result =
        transactionTracer
            .traceTransaction(blockHash, hash, execTracer)
            .map(DebugTraceTransactionResult::new)
            .orElse(null);
    return new JsonRpcSuccessResponse(request.getId(), result);
  }
}