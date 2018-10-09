package net.consensys.pantheon.ethereum.jsonrpc.websocket.subscription.blockheaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.consensys.pantheon.crypto.SECP256K1.KeyPair;
import net.consensys.pantheon.ethereum.chain.BlockAddedEvent;
import net.consensys.pantheon.ethereum.core.Block;
import net.consensys.pantheon.ethereum.core.BlockBody;
import net.consensys.pantheon.ethereum.core.BlockHeader;
import net.consensys.pantheon.ethereum.core.BlockHeaderTestFixture;
import net.consensys.pantheon.ethereum.core.Hash;
import net.consensys.pantheon.ethereum.core.TransactionTestFixture;
import net.consensys.pantheon.ethereum.jsonrpc.internal.queries.BlockWithMetadata;
import net.consensys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;
import net.consensys.pantheon.ethereum.jsonrpc.internal.queries.TransactionWithMetadata;
import net.consensys.pantheon.ethereum.jsonrpc.internal.results.BlockResult;
import net.consensys.pantheon.ethereum.jsonrpc.internal.results.BlockResultFactory;
import net.consensys.pantheon.ethereum.jsonrpc.internal.results.JsonRpcResult;
import net.consensys.pantheon.ethereum.jsonrpc.websocket.subscription.SubscriptionManager;
import net.consensys.pantheon.util.uint.UInt256;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NewBlockHeadersSubscriptionServiceTest {

  private NewBlockHeadersSubscriptionService newBlockHeadersSubscriptionService;

  @Captor ArgumentCaptor<Long> subscriptionIdCaptor;
  @Captor ArgumentCaptor<JsonRpcResult> responseCaptor;

  @Mock private SubscriptionManager subscriptionManager;
  @Mock private BlockchainQueries blockchainQueries;

  private final BlockHeaderTestFixture blockHeaderTestFixture = new BlockHeaderTestFixture();
  private final TransactionTestFixture txTestFixture = new TransactionTestFixture();
  private final BlockHeader blockHeader = blockHeaderTestFixture.buildHeader();
  private final BlockResultFactory blockResultFactory = new BlockResultFactory();

  @Before
  public void before() {
    newBlockHeadersSubscriptionService =
        new NewBlockHeadersSubscriptionService(subscriptionManager, blockchainQueries);
  }

  @Test
  public void shouldSendMessageWhenBlockAdded() {
    final NewBlockHeadersSubscription subscription = createSubscription(false);
    final BlockWithMetadata<Hash, Hash> testBlockWithMetadata =
        new BlockWithMetadata<>(
            blockHeader, Collections.emptyList(), Collections.emptyList(), UInt256.ONE, 1);
    final BlockResult expectedNewBlock = blockResultFactory.transactionHash(testBlockWithMetadata);

    when(blockchainQueries.blockByHashWithTxHashes(testBlockWithMetadata.getHeader().getHash()))
        .thenReturn(Optional.of(testBlockWithMetadata));

    simulateAddingBlock();

    verify(subscriptionManager)
        .sendMessage(subscriptionIdCaptor.capture(), responseCaptor.capture());
    final Long actualSubscriptionId = subscriptionIdCaptor.getValue();
    final Object actualBlock = responseCaptor.getValue();

    assertThat(actualSubscriptionId).isEqualTo(subscription.getId());
    assertThat(actualBlock).isEqualToComparingFieldByFieldRecursively(expectedNewBlock);

    verify(subscriptionManager, times(1)).sendMessage(any(), any());
  }

  @Test
  public void shouldReturnTxHashesWhenIncludeTransactionsFalse() {
    final NewBlockHeadersSubscription subscription = createSubscription(false);
    final List<Hash> txHashList = transactionsWithHashOnly();
    final BlockWithMetadata<Hash, Hash> testBlockWithMetadata =
        new BlockWithMetadata<>(blockHeader, txHashList, Collections.emptyList(), UInt256.ONE, 1);
    final BlockResult expectedNewBlock = blockResultFactory.transactionHash(testBlockWithMetadata);

    when(blockchainQueries.blockByHashWithTxHashes(testBlockWithMetadata.getHeader().getHash()))
        .thenReturn(Optional.of(testBlockWithMetadata));

    simulateAddingBlock();

    verify(subscriptionManager)
        .sendMessage(subscriptionIdCaptor.capture(), responseCaptor.capture());
    final Long actualSubscriptionId = subscriptionIdCaptor.getValue();
    final Object actualBlock = responseCaptor.getValue();

    assertThat(actualSubscriptionId).isEqualTo(subscription.getId());
    assertThat(actualBlock).isInstanceOf(BlockResult.class);
    final BlockResult actualBlockResult = (BlockResult) actualBlock;
    assertThat(actualBlockResult.getTransactions()).hasSize(txHashList.size());
    assertThat(actualBlock).isEqualToComparingFieldByFieldRecursively(expectedNewBlock);

    verify(subscriptionManager, times(1)).sendMessage(any(), any());
    verify(blockchainQueries, times(1)).blockByHashWithTxHashes(any());
    verify(blockchainQueries, times(0)).blockByHash(any());
  }

  @Test
  public void shouldReturnCompleteTxWhenParameterTrue() {
    final NewBlockHeadersSubscription subscription = createSubscription(true);
    final List<TransactionWithMetadata> txHashList = transactionsWithMetadata();
    final BlockWithMetadata<TransactionWithMetadata, Hash> testBlockWithMetadata =
        new BlockWithMetadata<>(
            blockHeader, txHashList, Collections.emptyList(), blockHeader.getDifficulty(), 0);
    final BlockResult expectedNewBlock =
        blockResultFactory.transactionComplete(testBlockWithMetadata);

    when(blockchainQueries.blockByHash(testBlockWithMetadata.getHeader().getHash()))
        .thenReturn(Optional.of(testBlockWithMetadata));

    simulateAddingBlock();

    verify(subscriptionManager)
        .sendMessage(subscriptionIdCaptor.capture(), responseCaptor.capture());
    final Long actualSubscriptionId = subscriptionIdCaptor.getValue();
    final Object actualBlock = responseCaptor.getValue();

    assertThat(actualSubscriptionId).isEqualTo(subscription.getId());
    assertThat(actualBlock).isInstanceOf(BlockResult.class);
    final BlockResult actualBlockResult = (BlockResult) actualBlock;
    assertThat(actualBlockResult.getTransactions()).hasSize(txHashList.size());
    assertThat(actualBlock).isEqualToComparingFieldByFieldRecursively(expectedNewBlock);

    verify(subscriptionManager, times(1)).sendMessage(any(), any());
    verify(blockchainQueries, times(0)).blockByHashWithTxHashes(any());
    verify(blockchainQueries, times(1)).blockByHash(any());
  }

  private void simulateAddingBlock() {
    final BlockBody blockBody = new BlockBody(Collections.emptyList(), Collections.emptyList());
    final Block testBlock = new Block(blockHeader, blockBody);
    newBlockHeadersSubscriptionService.onBlockAdded(
        BlockAddedEvent.createForHeadAdvancement(testBlock), blockchainQueries.getBlockchain());
    verify(blockchainQueries, times(1)).getBlockchain();
  }

  private List<TransactionWithMetadata> transactionsWithMetadata() {
    final TransactionWithMetadata t1 =
        new TransactionWithMetadata(
            txTestFixture.createTransaction(KeyPair.generate()), 0L, Hash.ZERO, 0);
    final TransactionWithMetadata t2 =
        new TransactionWithMetadata(
            txTestFixture.createTransaction(KeyPair.generate()), 1L, Hash.ZERO, 1);
    return Lists.newArrayList(t1, t2);
  }

  private List<Hash> transactionsWithHashOnly() {
    final List<Hash> hashes = new ArrayList<>();
    for (final TransactionWithMetadata transactionWithMetadata : transactionsWithMetadata()) {
      hashes.add(transactionWithMetadata.getTransaction().hash());
    }
    return hashes;
  }

  private NewBlockHeadersSubscription createSubscription(final boolean includeTransactions) {
    final NewBlockHeadersSubscription headerSub =
        new NewBlockHeadersSubscription(1L, includeTransactions);
    when(subscriptionManager.subscriptionsOfType(any(), any()))
        .thenReturn(Lists.newArrayList(headerSub));
    return headerSub;
  }
}