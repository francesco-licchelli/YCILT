package com.example.ycilt.workers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.work.*
import java.util.UUID

object WorkerManager {
	private lateinit var workManager: WorkManager

	data class WorkerData(
		val owner: LifecycleOwner,
		val workerClass: OneTimeWorkRequest.Builder,
		val id: UUID,
		val inputData: Data,
		val constraints: Constraints,
		val tags: List<String>,
		val onSucceeded: () -> Unit = {},
		val onFailed: () -> Unit = {},
	)

	private val deferredWorks =
		mutableListOf<WorkerData>() // Lista per tenere traccia dei lavori in attesa

	fun initialize(context: Context) {
		workManager = WorkManager.getInstance(context)
	}


	fun enqueueWorker(
		owner: LifecycleOwner,
		workerBuilderType: OneTimeWorkRequest.Builder,
		inputData: Data,
		constraints: Constraints,
		tags: List<String> = emptyList(),
		onSucceeded: () -> Unit = {},
		onFailed: () -> Unit = {},
		beforeWork: () -> Unit = {}
	) {
		beforeWork()
		val workBuilder = workerBuilderType
			.setInputData(inputData)
			.setConstraints(constraints)
		tags.forEach { tag -> workBuilder.addTag(tag) }
		val workerRequest = workBuilder.build()

		workManager.enqueue(workerRequest)
		observeWorkCompletion(
			owner,
			workerRequest.id,
			workerBuilderType,
			inputData,
			constraints,
			tags,
			onSucceeded,
			onFailed
		)
	}

	private fun observeWorkCompletion(
		owner: LifecycleOwner,
		workId: UUID,
		workerBuilder: OneTimeWorkRequest.Builder,
		inputData: Data,
		constraints: Constraints,
		tags: List<String>,
		onSucceeded: () -> Unit,
		onFailed: () -> Unit,
	) {
		val liveData: LiveData<WorkInfo?> = workManager.getWorkInfoByIdLiveData(workId)
		Handler(Looper.getMainLooper()).post {
			liveData.observe(owner) { workInfo ->
				workInfo?.let {
					if (it.state.isFinished) {
						when (it.state) {
							WorkInfo.State.SUCCEEDED -> {
								onSucceeded()
							}

							WorkInfo.State.FAILED -> {
								if (it.outputData.getBoolean("retry", false))
									deferredWorks.add(
										WorkerData(
											owner,
											workerBuilder,
											workId,
											inputData,
											constraints,
											tags
										)
									)
								else
									onFailed()
							}

							WorkInfo.State.CANCELLED -> {
								Log.w("WorkerManager", "Work with ID: $workId cancelled")
							}

							else -> {
								Log.w(
									"WorkerManager",
									"Work with ID: $workId is in an unknown state"
								)
							}
						}

					}
				}
			}
		}
	}

	fun resumeAll() {
		deferredWorks.toList().forEach {
			enqueueWorker(it.owner, it.workerClass, it.inputData, it.constraints, it.tags)
		}
		deferredWorks.clear()
	}
}
