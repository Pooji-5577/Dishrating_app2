package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.SocialRepository
import com.example.smackcheck2.data.repository.NotificationService
import com.example.smackcheck2.model.Comment
import com.example.smackcheck2.model.CommentsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CommentsViewModel(private val ratingId: String) : ViewModel() {

    private val socialRepository = SocialRepository()
    private val authRepository = AuthRepository()
    private val notificationService = NotificationService()

    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState.asStateFlow()

    init {
        loadComments()
    }

    fun loadComments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            socialRepository.getCommentsForRating(ratingId).fold(
                onSuccess = { comments ->
                    _uiState.update {
                        it.copy(comments = comments, isLoading = false)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load comments"
                        )
                    }
                }
            )
        }
    }

    fun addComment(content: String) {
        val parentId = _uiState.value.replyingTo?.id

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }

            // Ensure user profile exists
            val user = authRepository.getCurrentUser()
            if (user == null) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Please sign in to comment"
                    )
                }
                return@launch
            }

            socialRepository.addComment(
                ratingId = ratingId,
                userId = user.id,
                content = content,
                parentCommentId = parentId
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false, replyingTo = null) }
                    loadComments()
                    viewModelScope.launch {
                        notificationService.notifyCommentOnRating(
                            ratingId = ratingId,
                            commenterId = user.id,
                            commenterName = user.name
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = error.message ?: "Failed to post comment"
                        )
                    }
                }
            )
        }
    }

    fun setReplyingTo(comment: Comment?) {
        _uiState.update { it.copy(replyingTo = comment) }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            // Ensure user profile exists
            val user = authRepository.getCurrentUser()
            if (user == null) {
                _uiState.update {
                    it.copy(errorMessage = "Please sign in to delete comments")
                }
                return@launch
            }

            socialRepository.deleteComment(commentId, user.id).fold(
                onSuccess = { loadComments() },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Failed to delete comment")
                    }
                }
            )
        }
    }
}
