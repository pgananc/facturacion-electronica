part of 'referral_guide_bloc.dart';

abstract class ReferralGuideEvent extends Equatable {
  const ReferralGuideEvent();

  @override
  List<Object?> get props => [];
}

class LoadReferralGuidesEvent extends ReferralGuideEvent {
  final int? page;
  final int? pageSize;
  final int? idCompany;
  final bool forceRefresh;

  const LoadReferralGuidesEvent({
    this.page,
    this.pageSize,
    this.idCompany,
    this.forceRefresh = false,
  });

  @override
  List<Object?> get props => [page, pageSize, idCompany, forceRefresh];
}

class LoadReferralGuideByIdEvent extends ReferralGuideEvent {
  final int id;
  final int? idCompany;

  const LoadReferralGuideByIdEvent(this.id, {this.idCompany});

  @override
  List<Object?> get props => [id, idCompany];
}

class CreateReferralGuideEvent extends ReferralGuideEvent {
  final Map<String, dynamic> data;

  const CreateReferralGuideEvent(this.data);

  @override
  List<Object> get props => [data];
}

class ResetReferralGuideEvent extends ReferralGuideEvent {
  const ResetReferralGuideEvent();

  @override
  List<Object?> get props => [];
}
