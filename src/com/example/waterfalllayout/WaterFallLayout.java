package com.example.waterfalllayout;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Path.FillType;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

public class WaterFallLayout extends FrameLayout {
	private static final String TAG = "WaterFallLayout";
	private Paint paint;
	private Path path;
	private float mLastX = 0;
	private float mLastY = 0;
	private int maxPullHeight = dp2px(60);
	
	private float maxRadius = dp2px(15);
	
	private float curShowHeight = 0;
	private float radius;
	
	private boolean drawWaterOnWave =  true;
	
	private Path fallingWaterPath ;
	
	private ValueAnimator animator;
	
	private float waterFallPx = 0;
	
	private float waterFallPy = maxRadius * 2;
	
	private float waterFallEndPy = 0;
	
	private float pyDistance = 2 * maxRadius;
	
	private static final int NORMAL = 0;
	private static final int FALLING = 1;
	private static final int FALLEDN = 2;
	
	private int curStates = NORMAL;

	
	private Loading loading;
	
	public WaterFallLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	public WaterFallLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public WaterFallLayout(Context context) {
		super(context);
		init();
	}

	@SuppressLint("NewApi") private void init() {
		setWillNotDraw(false);
		initPaint();
		
		loading = new Loading();
		
		
		
		getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			
			@Override
			public void onGlobalLayout() {
				
				waterFallPx = getMeasuredWidth() / 2;
				
				waterFallEndPy = getMeasuredHeight() / 3;
				
				fallingWaterPath();
				
				getViewTreeObserver().removeOnGlobalLayoutListener(this);
				
			}
		});
	
		
	}

	private void fallingWaterPath() {
		fallingWaterPath = new Path();
		
		fallingWaterPath.setFillType(FillType.WINDING);
		fallingWaterPath.addCircle(waterFallPx, waterFallPy, maxRadius, Direction.CCW);
		
		
		fallingWaterPath.moveTo(waterFallPx, 0);
		
		float sinq = maxRadius / pyDistance;
		
		float cosq = (float) Math.sqrt(1- Math.pow(sinq, 2.0f));
		
		
		float leftPointX = waterFallPx - cosq * maxRadius;
		
		float leftPointY = waterFallPy - sinq * maxRadius;
		
		
		float rightPointX = waterFallPx + cosq * maxRadius;
		float rightPointY = waterFallPy - sinq * maxRadius;
		
		
		fallingWaterPath.lineTo(leftPointX, leftPointY);
		
		
		
		fallingWaterPath.lineTo(rightPointX, rightPointY);

		
	}
	
	private void initPaint() {
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStyle(Style.FILL);
		paint.setColor(0xffff0000);
		paint.setStrokeWidth(5);
		path = new Path();
	}
	
	
	
	@Override
	protected void onDraw(Canvas canvas) {
		
		canvas.drawPath(path, paint);
			
		if(drawWaterOnWave) {
			canvas.drawCircle(waterFallPx , curShowHeight - radius/2, radius, paint);
		} else if(curStates == FALLING){
			
		
			
			canvas.save();
			
			canvas.translate(0, waterFallPy);
			canvas.drawPath(fallingWaterPath, paint);
			
			canvas.restore();
		} else if(curStates == FALLEDN){
			if(!loading.isStart) {
				loading.start();
			}
			canvas.save();
			
			canvas.translate(waterFallPx - dp2px(17.5f), waterFallPy);
			
			
			paint.setStyle(Style.STROKE);
			
			loading.draw(canvas, paint);
			paint.setStyle(Style.FILL);
			canvas.restore();
		}
		

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			recordLastPos(event);
			drawWaterOnWave = true;
			invalidate();
			break;
		case MotionEvent.ACTION_MOVE:
			
			float moveDistance = calcMoveDistance(event);
			curShowHeight += moveDistance;
			if(curShowHeight < 0) {
				curShowHeight = 0;
			} else if(curShowHeight > maxPullHeight){
				curShowHeight = maxPullHeight;
			}

			updateDrawData();
			
			recordLastPos(event);
			
			invalidate();
			
			break;
		case MotionEvent.ACTION_UP:
			bounceBack();
			break;

		default:
			break;
		}
		
		return true;
	}
	
	private void bounceBack() {
	
		drawWaterOnWave =  curShowHeight < maxPullHeight;// 判断水滴掉不掉下来的
		
		// 回弹动画
		animator = ValueAnimator.ofFloat(curShowHeight,0);
		
		animator.setInterpolator(new BounceInterpolator());
		
		animator.setDuration(500);
		
		animator.addUpdateListener(new AnimatorUpdateListener() {
			
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
					float curValue = (Float) animation.getAnimatedValue();
					setCurShowHeight(curValue);
			}
		});

		animator.start();
		// 水滴下落动画
		if(!drawWaterOnWave) {
			ValueAnimator fallAnimator = ValueAnimator.ofFloat(curShowHeight - radius/2,waterFallEndPy);
			
			fallAnimator.setDuration(500);
			
			fallAnimator.setInterpolator(new AccelerateInterpolator());
			
			fallAnimator.addUpdateListener(new AnimatorUpdateListener() {
				
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					
					waterFallPy = (Float) animation.getAnimatedValue(); // 画布移动的距离
					curStates = FALLING;
					invalidate();
				}
			});
			
			fallAnimator.addListener(new AnimatorListener() {
				
				@Override
				public void onAnimationStart(Animator animation) {
					
				}
				
				@Override
				public void onAnimationRepeat(Animator animation) {
					
				}
				
				@Override
				public void onAnimationEnd(Animator animation) {
					curStates = FALLEDN;
					invalidate();
				}
				
				@Override
				public void onAnimationCancel(Animator animation) {
					
				}
			});
			
			fallAnimator.start();
		}
			
	}
	private void updateDrawData() {
		
		radius = curShowHeight / maxPullHeight * maxRadius;
		
		int width = getMeasuredWidth();
		
		path.reset();

		path.moveTo(0, 0);

		path.quadTo(width / 4, 0, width / 2, curShowHeight + radius/4);

		path.quadTo(width * 3 / 4, 0, width, 0);
	}
	private void setCurShowHeight(float curShowHeight) {
		
		this.curShowHeight = curShowHeight;
		
		updateDrawData();
		
		invalidate();
	}
	
	
	private float calcMoveDistance(MotionEvent event) {
		return event.getY() - mLastY;
	}
	
	private void recordLastPos(MotionEvent event) {
		 mLastX = event.getX();
		 mLastY = event.getY();
	}
	
	@SuppressWarnings("unused")
	private int dp2px(float dp) {

		WindowManager wm = (WindowManager) getContext().getSystemService(
				Context.WINDOW_SERVICE);

		DisplayMetrics outMetrics = new DisplayMetrics();

		wm.getDefaultDisplay().getMetrics(outMetrics);

	
		return (int) ((outMetrics.density * dp) + 0.5f);
		
	}
	
	
	private class Loading {
		private int radius = dp2px(35);
		
		private ValueAnimator startAnimator;
		private ValueAnimator endAnimator;
		private ValueAnimator offsetAnimator;
		
		private float startAngle = 0;
		
		private float endAngle = 0;
		
		private float offsetAngle = 0;
		
		boolean isStart = false;
		private RectF rectF = new RectF(0,0,radius,radius);
		public Loading() {
			init();
		}

		private void init() {
			
			startAnimator = ValueAnimator.ofFloat(0,360.0f);
			startAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
			startAnimator.setDuration(1000);
			startAnimator.setRepeatCount(0);
			
			endAnimator = startAnimator.clone();
			
			
			offsetAnimator = ValueAnimator.ofFloat(0,360.0f);
			offsetAnimator.setInterpolator(new LinearInterpolator());
			offsetAnimator.setDuration(5000);
			offsetAnimator.setRepeatCount(ValueAnimator.INFINITE);
			
		}
		
		void draw(Canvas canvas,Paint paint) {
			
			
			float sweepAngle = endAngle - startAngle;
			
			if(sweepAngle < 0) {
				sweepAngle += 360;
			}
			canvas.drawArc(rectF, startAngle + offsetAngle, sweepAngle, false, paint);
		}
		
		void start() {
			if(isStart) {
				return;
			}
			
			isStart = true;
			startAnimator.addListener(new AnimatorListener() {
				@Override
				public void onAnimationStart(Animator animation) {
					
					postDelayed(new Runnable() {
						
						@Override
						public void run() {
							endAnimator.start();
						}
					}, 400);
					
				}
				
				@Override
				public void onAnimationRepeat(Animator animation) {
					
				}
				
				@Override
				public void onAnimationEnd(Animator animation) {
					
				}
				
				@Override
				public void onAnimationCancel(Animator animation) {
					
				}
			});
			
			endAnimator.addListener(new AnimatorListener() {
				
				@Override
				public void onAnimationStart(Animator animation) {
					
				}
				
				@Override
				public void onAnimationRepeat(Animator animation) {
					
				}
				
				@Override
				public void onAnimationEnd(Animator animation) {
					startAnimator.start();
				}
				
				@Override
				public void onAnimationCancel(Animator animation) {
					
				}
			});
			
			startAnimator.addUpdateListener(new AnimatorUpdateListener() {
				
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					
					endAngle = ((Float) animation.getAnimatedValue()); // 
					invalidate();
				}
			});
			
			endAnimator.addUpdateListener(new AnimatorUpdateListener() {
				
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					
					startAngle = (Float)animation.getAnimatedValue();
					
					invalidate();
					
				}
			});
			
			
			offsetAnimator.addUpdateListener(new AnimatorUpdateListener() {
				
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					
					offsetAngle = (Float) animation.getAnimatedValue();
									
					invalidate();
					
				}
			});
			
			
			
			startAnimator.start();
			offsetAnimator.start();
			
		}
		void stop() {
			
			if(!isStart) {
				return;
			}
			isStart = false;
			
			startAnimator.cancel();
			endAnimator.cancel();
			offsetAnimator.cancel();
		}
		

		
	}
	
}




























