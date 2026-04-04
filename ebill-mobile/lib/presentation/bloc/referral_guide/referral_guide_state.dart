part of 'referral_guide_bloc.dart';

abstract class ReferralGuideState extends Equatable {
  const ReferralGuideState();

  @override
  List<Object?> get props => [];
}

class ReferralGuideInitial extends ReferralGuideState {}

class ReferralGuideLoading extends ReferralGuideState {}

class ReferralGuideDetailLoading extends ReferralGuideState {}

class ReferralGuideLoaded extends ReferralGuideState {
  final List<ReferralGuide> guides;

  const ReferralGuideLoaded(this.guides);

  @override
  List<Object> get props => [guides];
}

class ReferralGuideDetailLoaded extends ReferralGuideState {
  final ReferralGuide guide;

  const ReferralGuideDetailLoaded(this.guide);

  @override
  List<Object> get props => [guide];
}

class ReferralGuideCreated extends ReferralGuideState {
  final ReferralGuide guide;

  const ReferralGuideCreated(this.guide);

  @override
  List<Object> get props => [guide];
}

class ReferralGuideListError extends ReferralGuideState {
  final String message;

  const ReferralGuideListError(this.message);

  @override
  List<Object?> get props => [message];
}

class ReferralGuideDetailError extends ReferralGuideState {
  final String message;

  const ReferralGuideDetailError(this.message);

  @override
  List<Object?> get props => [message];
}

class ReferralGuideFormError extends ReferralGuideState {
  final String message;

  const ReferralGuideFormError(this.message);

  @override
  List<Object?> get props => [message];
}
