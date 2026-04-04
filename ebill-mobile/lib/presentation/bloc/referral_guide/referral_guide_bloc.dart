import 'package:equatable/equatable.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../domain/entities/referral_guide.dart';
import '../../../domain/repositories/referral_guide_repository.dart';

part 'referral_guide_event.dart';
part 'referral_guide_state.dart';

class ReferralGuideBloc extends Bloc<ReferralGuideEvent, ReferralGuideState> {
  final ReferralGuideRepository referralGuideRepository;

  ReferralGuideBloc(ReferralGuideRepository repository)
      : referralGuideRepository = repository,
        super(ReferralGuideInitial()) {
    on<LoadReferralGuidesEvent>(_onLoadReferralGuides);
    on<LoadReferralGuideByIdEvent>(_onLoadReferralGuideById);
    on<CreateReferralGuideEvent>(_onCreateReferralGuide);
    on<ResetReferralGuideEvent>(
      (event, emit) => emit(ReferralGuideInitial()),
    );
  }

  Future<void> _onLoadReferralGuides(
    LoadReferralGuidesEvent event,
    Emitter<ReferralGuideState> emit,
  ) async {
    if (!event.forceRefresh && state is ReferralGuideLoaded) return;
    emit(ReferralGuideLoading());
    final result = await referralGuideRepository.getReferralGuides(
      page: event.page,
      pageSize: event.pageSize,
      idCompany: event.idCompany,
    );
    result.fold(
      (failure) => emit(ReferralGuideListError(failure.message)),
      (guides) => emit(ReferralGuideLoaded(guides)),
    );
  }

  Future<void> _onLoadReferralGuideById(
    LoadReferralGuideByIdEvent event,
    Emitter<ReferralGuideState> emit,
  ) async {
    emit(ReferralGuideDetailLoading());
    final result = await referralGuideRepository.getReferralGuideById(
      event.id,
      idCompany: event.idCompany,
    );
    result.fold(
      (failure) => emit(ReferralGuideDetailError(failure.message)),
      (guide) => emit(ReferralGuideDetailLoaded(guide)),
    );
  }

  Future<void> _onCreateReferralGuide(
    CreateReferralGuideEvent event,
    Emitter<ReferralGuideState> emit,
  ) async {
    emit(ReferralGuideLoading());
    final result = await referralGuideRepository.createReferralGuide(event.data);
    result.fold(
      (failure) => emit(ReferralGuideFormError(failure.message)),
      (guide) => emit(ReferralGuideCreated(guide)),
    );
  }
}
