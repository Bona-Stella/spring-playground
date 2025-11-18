package com.github.stella.springaoplab.tx;

import com.github.stella.springaoplab.post.domain.Post;
import com.github.stella.springaoplab.post.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TxBoundaryService {

    private final PostRepository postRepository;

    public TxBoundaryService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    // 1) RuntimeException → default rollback
    @Transactional
    public void createThenRuntimeException(String prefix) {
        postRepository.save(new Post(prefix + "-A", "runtime ex demo"));
        throw new RuntimeException("boom-runtime");
    }

    // 2) Checked Exception → by default, commit (no rollback)
    @Transactional
    public void createThenCheckedException(String prefix) throws Exception {
        postRepository.save(new Post(prefix + "-B", "checked ex demo (no rollback by default)"));
        throw new Exception("boom-checked");
    }

    // 3) Checked Exception but with rollbackFor → rollback
    @Transactional(rollbackFor = Exception.class)
    public void createThenCheckedExceptionWithRollback(String prefix) throws Exception {
        postRepository.save(new Post(prefix + "-C", "checked ex with rollbackFor demo"));
        throw new Exception("boom-checked-rollback");
    }

    // 4) REQUIRED → REQUIRED (same Tx). Inner throws → all rolled back
    @Transactional
    public void outerRequired_calls_innerRequired_fail(String prefix) {
        postRepository.save(new Post(prefix + "-OUTER-R", "outer REQUIRED"));
        innerRequiredFail(prefix);
    }

    @Transactional
    protected void innerRequiredFail(String prefix) {
        postRepository.save(new Post(prefix + "-INNER-R", "inner REQUIRED then fail"));
        throw new RuntimeException("inner-required-fail");
    }

    // 5) REQUIRED → REQUIRES_NEW. Inner fails and rolls back; outer can commit if it catches
    @Transactional
    public void outerRequired_calls_innerRequiresNew_fail_but_catch(String prefix) {
        postRepository.save(new Post(prefix + "-OUTER-RN", "outer REQUIRED before REQUIRES_NEW"));
        try {
            innerRequiresNewFail(prefix);
        } catch (RuntimeException ignore) {
            // swallow to let outer transaction commit
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void innerRequiresNewFail(String prefix) {
        postRepository.save(new Post(prefix + "-INNER-RN", "inner REQUIRES_NEW then fail"));
        throw new RuntimeException("inner-requires-new-fail");
    }

    // 6) readOnly transaction write attempt (provider dependent)
    @Transactional(readOnly = true)
    public void readOnlyWriteAttempt(String prefix) {
        // In Hibernate, readOnly often sets FlushMode.MANUAL, so this save may not be flushed
        postRepository.save(new Post(prefix + "-RO", "readOnly write attempt"));
        // no flush; method ends with readOnly=true, so typically nothing is persisted
    }
}
